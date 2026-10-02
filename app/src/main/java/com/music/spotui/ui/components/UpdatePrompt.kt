package com.music.spotui.ui.components

import android.content.Intent
import com.music.spotui.ui.theme.SoloShape
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.data.update.UpdateChecker
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlin.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.Surface3

/** Download/install lifecycle for the in-app updater. */
private enum class UpdatePhase { IDLE, DOWNLOADING, READY, ERROR }

@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // One silent check on launch publishes into the shared UpdateState, which also lights the
    // red dot on Home and in Settings. The dialog then renders whatever UpdateState holds, so a
    // manual "Check for updates" from Settings opens this same premium prompt too.
    LaunchedEffect(Unit) {
        UpdateChecker.check(context)
    }

    val info = com.music.spotui.data.update.UpdateState.available
    // Local dismissal: "Later" hides the dialog without clearing the shared badge, so the red dot
    // stays until the user installs or taps "Don't show again". A Settings > Updates tap bumps
    // UpdateState.showRequest, which clears the local dismissal so the dialog re-opens on demand.
    var dismissed by remember { mutableStateOf(false) }
    val showRequest = com.music.spotui.data.update.UpdateState.showRequest
    LaunchedEffect(showRequest) { if (showRequest > 0) dismissed = false }
    LaunchedEffect(info?.fingerprint) { dismissed = false }
    if (info == null || dismissed) return
    fun dismissDialog() { dismissed = true }

    var phase by remember { mutableStateOf(UpdatePhase.IDLE) }
    var progress by remember { mutableStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadedApk by remember { mutableStateOf<java.io.File?>(null) }
    var downloadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Unknown-sources gate: when the user returns from the settings screen and the permission
    // is now granted, the pending install is launched automatically.
    val installPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        downloadedApk?.let { apk ->
            if (UpdateChecker.canInstall(context)) {
                UpdateChecker.installApk(context, apk)
                com.music.spotui.data.update.UpdateState.clear()
            }
        }
    }

    // Downloads the apk in-app, then either installs it (permission ready) or routes to the
    // unknown-sources screen first. NEVER a browser redirect for the APK.
    val startUpdate: () -> Unit = start@{
        // Defensive: if the release only exposed an html page (no .apk asset), fall back to the
        // system installer flow isn't possible — but we still never hard-redirect to a browser;
        // the dialog simply reports it so the user can grab it from the release page they chose.
        if (!UpdateChecker.isApkUrl(info.downloadUrl)) {
            errorMessage = "No installable APK was attached to this release."
            phase = UpdatePhase.ERROR
            return@start
        }
        phase = UpdatePhase.DOWNLOADING
        progress = 0f
        errorMessage = null
        downloadJob = scope.launch {
            val result = UpdateChecker.downloadApk(context, info.downloadUrl) { p -> progress = p }
            when (result) {
                is UpdateChecker.DownloadResult.Success -> {
                    downloadedApk = result.apk
                    phase = UpdatePhase.READY
                    if (UpdateChecker.canInstall(context)) {
                        UpdateChecker.installApk(context, result.apk)
                        com.music.spotui.data.update.UpdateState.clear()
                    } else {
                        // Ask the OS to let SOLO install, then resume in the launcher callback.
                        runCatching { installPermissionLauncher.launch(UpdateChecker.unknownSourcesIntent(context)) }
                    }
                }
                is UpdateChecker.DownloadResult.Failure -> {
                    if (result.reason != "Cancelled") {
                        errorMessage = result.reason
                        phase = UpdatePhase.ERROR
                    } else {
                        phase = UpdatePhase.IDLE
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (phase != UpdatePhase.DOWNLOADING) dismissDialog() },
        // Platform default width keeps the prompt a compact product dialog rather than a
        // full-bleed sheet; the markdown block scrolls inside it.
        modifier = Modifier.width(360.dp),
        shape = SoloShape.xl,
        containerColor = Surface2,
        titleContentColor = TextPrimary,
        title = {
            // Premium header: the SOLO mark on an accent-tinted well beside the new version,
            // so the prompt reads as the app's own update rather than a generic system dialog.
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                // The SOLO mark on an accent gradient plate with a soft accent glow, so the
                // prompt reads as a crafted, premium product surface, not a generic dialog.
                Box(
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(SoloShape.md)
                        .background(com.music.spotui.ui.theme.AccentBrush)
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                            ),
                        ),
                ) {
                    com.music.spotui.ui.components.SoloMark(height = 26.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "UPDATE AVAILABLE",
                        color = Accent,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "SOLO ${info.version}",
                        color = TextPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "What's new",
                    color = TextPrimary,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                if (info.releaseBody.isNotBlank()) {
                    RenderMarkdown(info.releaseBody)
                } else {
                    Text(
                        "A new version of SOLO is available.",
                        color = TextSecondary,
                    )
                }
                if (phase == UpdatePhase.DOWNLOADING) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (progress >= 0f) "Downloading ${(progress * 100).toInt()}%" else "Downloading",
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (progress >= 0f) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(SoloShape.pill),
                            color = Accent,
                            trackColor = Surface3,
                        )
                    } else {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(SoloShape.pill),
                            color = Accent,
                            trackColor = Surface3,
                        )
                    }
                }
                if (phase == UpdatePhase.READY) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Downloaded. Follow the system prompt to install.",
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    )
                }
                errorMessage?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it, color = com.music.spotui.ui.theme.Danger, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                }
                // A short, calm note so the Android Play Protect safety scan and the system
                // install screen don't surprise anyone: both are a normal step for apps
                // installed outside the Play Store, not a sign anything is wrong. No alarming
                // wording, and no em-dash.
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoloShape.sm)
                        .background(Surface3)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        "Installing from outside the Play Store, Android may run a quick Play Protect safety check and show its own install screen. This is a normal security step, so just follow the prompt to finish.",
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = {
            when (phase) {
                UpdatePhase.DOWNLOADING -> SoloDialogConfirm(text = "Cancel", onClick = {
                    downloadJob?.cancel()
                    phase = UpdatePhase.IDLE
                })
                UpdatePhase.READY -> SoloDialogConfirm(text = "Install", onClick = {
                    downloadedApk?.let { apk ->
                        if (UpdateChecker.canInstall(context)) {
                            UpdateChecker.installApk(context, apk)
                            com.music.spotui.data.update.UpdateState.clear()
                        } else {
                            runCatching { installPermissionLauncher.launch(UpdateChecker.unknownSourcesIntent(context)) }
                        }
                    }
                })
                UpdatePhase.ERROR -> SoloDialogConfirm(text = "Retry", onClick = startUpdate)
                else -> SoloDialogConfirm(text = "Update now", onClick = startUpdate)
            }
        },
        dismissButton = {
            if (phase != UpdatePhase.DOWNLOADING) {
                TextButton(onClick = {
                    UpdateChecker.skipRelease(context, info)
                    dismissDialog()
                }) {
                    Text("Don't show again", color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                }
                SoloDialogDismiss(text = "Later", onClick = { dismissDialog() })
            }
        },
    )
}

private val HEADER_RE = Regex("""^(#{1,6})\s+(.*)""")

private data class ImageItem(val url: String, val widthPercent: Float?)

private fun extractImages(text: String): List<ImageItem> {
    val list = mutableListOf<ImageItem>()
    val imgTags = Regex("""<img\s+[^>]+>""", RegexOption.IGNORE_CASE).findAll(text)
    for (match in imgTags) {
        val tagContent = match.value
        val srcMatch = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(tagContent)
        val widthMatch = Regex("""width=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(tagContent)
        if (srcMatch != null) {
            val url = srcMatch.groupValues[1]
            val widthStr = widthMatch?.groupValues?.get(1)
            val widthPercent = widthStr?.removeSuffix("%")?.toFloatOrNull()?.let { it / 100f }
            list.add(ImageItem(url, widthPercent))
        }
    }
    val mdImgTags = Regex("""!\[([^\]]*)\]\(([^)]+)\)""").findAll(text)
    for (match in mdImgTags) {
        val url = match.groupValues[2]
        list.add(ImageItem(url, null))
    }
    return list
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun RenderImagesRow(images: List<ImageItem>, onImageClick: (String) -> Unit) {
    val aspectRatio = when (images.size) {
        1 -> 1.77f
        2 -> 1f
        else -> 0.6f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        images.forEach { image ->
            val weight = image.widthPercent ?: (1f / images.size)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .aspectRatio(aspectRatio)
                    .clip(SoloShape.sm)
                    .clickable { onImageClick(image.url) }
            ) {
                GlideImage(
                    model = image.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    failure = placeholder(com.music.spotui.R.drawable.placeholder),
                    loading = placeholder(com.music.spotui.R.drawable.placeholder),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ZoomableImageDialog(url: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offset += offsetChange
            } else {
                offset = androidx.compose.ui.geometry.Offset.Zero
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                }
        ) {
            GlideImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun RenderMarkdown(markdown: String) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val linkColor = Accent
    val headingColor = TextPrimary
    val bodyColor = TextSecondary

    var enlargedImageUrl by remember { mutableStateOf<String?>(null) }

    val lines = markdown.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // Look ahead for image block
        val currentImages = mutableListOf<ImageItem>()
        var j = i
        var hasImages = false
        while (j < lines.size) {
            val nextLine = lines[j].trim()
            val nextImages = extractImages(nextLine)
            val isImageContainerTag = nextLine.startsWith("<p", ignoreCase = true) || 
                                      nextLine.startsWith("</p", ignoreCase = true) ||
                                      nextLine.startsWith("<div", ignoreCase = true) ||
                                      nextLine.startsWith("</div", ignoreCase = true)
            
            if (nextImages.isNotEmpty()) {
                currentImages.addAll(nextImages)
                hasImages = true
                j++
            } else if (isImageContainerTag || nextLine.isEmpty()) {
                j++
            } else {
                break
            }
        }

        if (hasImages && currentImages.isNotEmpty()) {
            RenderImagesRow(currentImages, onImageClick = { enlargedImageUrl = it })
            i = j
            continue
        }

        when {
            // Generated "Full Changelog" footers only point at repository compare pages.
            trimmed.startsWith("**Full Changelog**") -> Unit
            trimmed.isEmpty() -> {
                Spacer(modifier = Modifier.height(6.dp))
            }
            trimmed.startsWith("<p", ignoreCase = true) || 
            trimmed.startsWith("</p", ignoreCase = true) ||
            trimmed.startsWith("<div", ignoreCase = true) ||
            trimmed.startsWith("</div", ignoreCase = true) -> {
                // Ignore container tags
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___"
                || (trimmed.length >= 3 && trimmed.all { it == '_' || it == '-' || it == '*' }) -> {
                HorizontalDivider(
                    color = Surface3,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            HEADER_RE.matches(trimmed) -> {
                val match = HEADER_RE.find(trimmed)!!
                val level = match.groupValues[1].length
                val (size, weight) = when (level) {
                    1 -> 18.sp to FontWeight.ExtraBold
                    2 -> 16.sp to FontWeight.Bold
                    3 -> 15.sp to FontWeight.SemiBold
                    4 -> 14.sp to FontWeight.SemiBold
                    5 -> 13.sp to FontWeight.SemiBold
                    else -> 13.sp to FontWeight.Bold
                }
                Text(
                    text = match.groupValues[2],
                    color = headingColor,
                    fontSize = size,
                    fontWeight = weight,
                    modifier = Modifier.padding(top = if (i > 0) 10.dp else 0.dp),
                )
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val item = trimmed.removePrefix("- ").removePrefix("* ")
                MarkdownInline(
                    text = item,
                    prefix = "\u2022 ",
                    linkColor = linkColor,
                    bodyColor = bodyColor,
                    uriHandler = uriHandler,
                )
            }
            Regex("""^\d+\.\s""").containsMatchIn(trimmed) -> {
                val numMatch = Regex("""^(\d+\.\s)(.*)""").find(trimmed)!!
                MarkdownInline(
                    text = numMatch.groupValues[2],
                    prefix = numMatch.groupValues[1],
                    linkColor = linkColor,
                    bodyColor = bodyColor,
                    uriHandler = uriHandler,
                )
            }
            else -> {
                MarkdownInline(
                    text = trimmed,
                    prefix = "",
                    linkColor = linkColor,
                    bodyColor = bodyColor,
                    uriHandler = uriHandler,
                )
            }
        }
        i++
    }

    enlargedImageUrl?.let { url ->
        ZoomableImageDialog(url = url, onDismiss = { enlargedImageUrl = null })
    }
}

@Composable
private fun MarkdownInline(
    text: String,
    prefix: String,
    linkColor: Color,
    bodyColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    val patterns = listOf(
        Triple(Regex("""\*\*(.+?)\*\*"""), "bold", null as String?),
        Triple(Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)"""), "italic", null as String?),
        Triple(Regex("""__(.+?)__"""), "underline", null as String?),
        Triple(Regex("""_(.+?)_"""), "underline", null as String?),
        Triple(Regex("""~~(.+?)~~"""), "strikethrough", null as String?),
        Triple(Regex("""`([^`]+)`"""), "code", null as String?),
        Triple(Regex("""\[([^\]]+)\]\(([^)]+)\)"""), "link", null as String?),
        Triple(Regex("""<a\s+[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE), "html_link", null as String?),
        Triple(Regex("""(?<!\()(https?://[^\s<)]+)"""), "raw_link", null as String?),
    )

    val annotated = buildAnnotatedString {
        if (prefix.isNotEmpty()) {
            pushStyle(SpanStyle(color = bodyColor))
            append(prefix)
            pop()
        }

        data class Segment(val start: Int, val end: Int, val type: String, val content: String, val url: String? = null)

        val segments = mutableListOf<Segment>()
        for ((regex, type, _) in patterns) {
            for (m in regex.findAll(text)) {
                segments.add(
                    when (type) {
                        "link" -> Segment(m.range.first, m.range.last + 1, type, m.groupValues[1], m.groupValues[2])
                        "html_link" -> Segment(m.range.first, m.range.last + 1, "link", m.groupValues[2], m.groupValues[1])
                        "raw_link" -> Segment(m.range.first, m.range.last + 1, "link", m.groupValues[1], m.groupValues[1])
                        else -> Segment(m.range.first, m.range.last + 1, type, m.groupValues[1])
                    }
                )
            }
        }

        val sorted = segments.sortedBy { it.start }.distinctBy { it.start }

        var cursor = 0
        for (seg in sorted) {
            if (seg.start < cursor) continue
            if (seg.start > cursor) {
                pushStyle(SpanStyle(color = bodyColor))
                append(text.substring(cursor, seg.start))
                pop()
            }
            when (seg.type) {
                "bold" -> {
                    pushStyle(SpanStyle(color = bodyColor, fontWeight = FontWeight.Bold))
                    append(seg.content)
                    pop()
                }
                "italic" -> {
                    pushStyle(SpanStyle(color = bodyColor, fontStyle = FontStyle.Italic))
                    append(seg.content)
                    pop()
                }
                "underline" -> {
                    pushStyle(SpanStyle(color = bodyColor, textDecoration = TextDecoration.Underline))
                    append(seg.content)
                    pop()
                }
                "strikethrough" -> {
                    pushStyle(SpanStyle(color = bodyColor, textDecoration = TextDecoration.LineThrough))
                    append(seg.content)
                    pop()
                }
                "code" -> {
                    pushStyle(SpanStyle(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    ))
                    append(seg.content)
                    pop()
                }
                // Repository links (commits, compares, PRs) render as plain text: the app
                // doesn't send people to source pages from its release notes.
                "link" -> if (seg.url.orEmpty().contains("github.com", ignoreCase = true)) {
                    pushStyle(SpanStyle(color = bodyColor))
                    append(seg.content)
                    pop()
                } else {
                    pushStringAnnotation(tag = "URL", annotation = seg.url ?: "")
                    pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                    append(seg.content)
                    pop()
                    pop()
                }
            }
            cursor = seg.end
        }

        if (cursor < text.length) {
            pushStyle(SpanStyle(color = bodyColor))
            append(text.substring(cursor))
            pop()
        }
    }

    ClickableText(
        text = annotated,
        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        modifier = Modifier.padding(vertical = 2.dp),
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { uriHandler.openUri(it.item) }
        },
    )
}
