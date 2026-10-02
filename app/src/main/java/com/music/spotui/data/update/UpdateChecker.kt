package com.music.spotui.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.music.spotui.BuildConfig
import com.music.spotui.data.preferences.getUpdateRepoUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val PREFS = "update_prefs"
    private const val KEY_SKIP = "skip_fingerprint"

    /** Cache subdir the release .apk is downloaded into; exposed via FileProvider @xml/file_paths. */
    private const val DOWNLOAD_DIR = "downloads"
    private const val APK_MIME = "application/vnd.android.package-archive"

    data class UpdateInfo(
        val version: String,
        val downloadUrl: String,
        val fingerprint: String,
        val releaseBody: String,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Longer read timeout for the APK body itself (the metadata client stays snappy at 8s).
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun check(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val result = resolve(context)
        // Publish into the shared app state so the red dot badge (Home, Settings) and the
        // update dialog all react to the same single result.
        UpdateState.set(result)
        result
    }

    private fun resolve(context: Context): UpdateInfo? {
        val info = runCatching { fetchLatestRelease(context) }
            .onFailure { Log.d(TAG, "update check failed: ${it.message}") }
            .getOrNull() ?: return null
        if (!isNewer(info.version, BuildConfig.VERSION_NAME)) return null
        val skipped = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SKIP, null)
        if (skipped == info.fingerprint) return null
        return info
    }

    fun skipRelease(context: Context, info: UpdateInfo) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SKIP, info.fingerprint).apply()
        // The user chose to dismiss this release, so clear the shared badge too.
        UpdateState.clear()
    }

    private fun fetchLatestRelease(context: Context): UpdateInfo? {
        val repoUrl = getUpdateRepoUrl(context).trimEnd('/')
        val repoPath = repoUrl.removePrefix("https://github.com/").removePrefix("http://github.com/")
        val apiLatest = "https://api.github.com/repos/$repoPath/releases/latest"
        val releasesPage = "$repoUrl/releases/latest"

        val request = Request.Builder()
            .url(apiLatest)
            .header("Accept", "application/vnd.github+json")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val tag = json.optString("tag_name").trim()
        if (tag.isBlank()) return null
        val version = extractVersion(tag)
            ?: return null
        val releaseBody = json.optString("body", "")
        val htmlUrl = json.optString("html_url", releasesPage)
            .ifBlank { releasesPage }
        val assets = json.optJSONArray("assets")
        val apkUrl = (0 until (assets?.length() ?: 0))
            .asSequence()
            .mapNotNull { assets?.optJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk") }
            ?.optString("browser_download_url")
        return UpdateInfo(
            version = version,
            downloadUrl = apkUrl?.ifBlank { null } ?: htmlUrl,
            fingerprint = "${json.optLong("id")}:${json.optString("updated_at")}:$version",
            releaseBody = releaseBody,
        )
    }

    private fun extractVersion(text: String): String? =
        Regex("""\d+(?:\.\d+)+""").find(text)?.value

    private fun isNewer(remote: String, installed: String): Boolean {
        val r = remote.split('.').map { it.toIntOrNull() ?: 0 }
        val i = installed.split('-', '+').first().split('.').map { it.toIntOrNull() ?: 0 }
        for (n in 0 until maxOf(r.size, i.size)) {
            val a = r.getOrElse(n) { 0 }
            val b = i.getOrElse(n) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    // ── In-app download + install (NO browser redirect) ──────────────────────────

    /** Outcome of a download attempt: either the finished .apk file or a failure. */
    sealed class DownloadResult {
        data class Success(val apk: File) : DownloadResult()
        data class Failure(val reason: String) : DownloadResult()
    }

    /** True when the update's download URL is a GitHub-release .apk asset we can install in-app. */
    fun isApkUrl(url: String): Boolean = url.endsWith(".apk", ignoreCase = true)

    /**
     * Downloads the release .apk into cache/downloads/ using the EXISTING OkHttp stack (no new HTTP
     * dependency), reporting progress in [0f, 1f] (or -1f when the server sends no content length).
     * Runs on IO. Returns the finished file or a failure reason; cancellation is cooperative via the
     * calling coroutine (the stream copy checks for interruption).
     */
    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit,
    ): DownloadResult = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, DOWNLOAD_DIR).apply { mkdirs() }
            // Stable name per version keeps the dir from filling with stale downloads.
            val target = File(dir, "SOLO-update.apk")
            if (target.exists()) target.delete()

            val request = Request.Builder().url(url).build()
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult.Failure("Server returned ${response.code}")
                }
                val body = response.body
                val total = body.contentLength()
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                                target.delete()
                                return@withContext DownloadResult.Failure("Cancelled")
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(if (total > 0) (downloaded.toFloat() / total) else -1f)
                        }
                        output.flush()
                    }
                }
            }
            onProgress(1f)
            DownloadResult.Success(target)
        }.getOrElse {
            Log.d(TAG, "apk download failed: ${it.message}")
            DownloadResult.Failure(it.message ?: "Download failed")
        }
    }

    /** Whether the OS will let this app install packages without first visiting unknown-sources. */
    fun canInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true

    /** Intent that opens this app's "allow from this source" screen (API 26+). */
    fun unknownSourcesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /**
     * Launches the SYSTEM package installer for the downloaded [apk] via the FileProvider content
     * uri (authority ${applicationId}.fileprovider) with a read-uri grant. Never opens a browser.
     * Returns false if even the installer intent could not be started.
     */
    fun installApk(context: Context, apk: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrElse {
        Log.d(TAG, "installer launch failed: ${it.message}")
        false
    }
}
