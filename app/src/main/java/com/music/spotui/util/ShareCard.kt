package com.music.spotui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.music.spotui.data.entity.SongsModel
import java.io.File

/**
 * Builds and shares a "now listening" artwork card: the track's cover art, its title and
 * artist, and the SOLO wordmark on the azure gradient, rendered to a PNG in the app cache and
 * handed to the system share sheet as an image via the app's FileProvider.
 *
 * This is a self-contained Android-Canvas render (no Compose capture, no new dependency). The
 * caller must invoke [shareSongArtworkCard] off the main thread because the cover art is pulled
 * synchronously from Glide; it returns false when the artwork can't be fetched so the caller
 * can fall back to the existing text-link share.
 */
object ShareCard {

    // Graphite & Azure palette, mirrored here as plain ints so the renderer stays framework-free.
    private const val CANVAS = 0xFF0E1013.toInt()
    private const val SURFACE = 0xFF1B1F24.toInt()
    private const val ACCENT = 0xFF3B82F6.toInt()      // refined azure
    private const val ACCENT_SOFT = 0xFF93C5FD.toInt()
    private const val ON_ACCENT = 0xFF0A0E14.toInt()
    private const val TEXT_PRIMARY = 0xFFF3F2FA.toInt()
    private const val TEXT_SECONDARY = 0xFFAEADC2.toInt()

    private const val W = 1080
    private const val H = 1350
    private const val PAD = 84f
    private const val ART = W - PAD * 2  // square cover

    /**
     * Renders the share card for [song] and launches the share sheet with it as an image.
     * Returns true on success; false (nothing launched) when the cover art is unavailable so
     * the caller can fall back to a text share. Must be called off the main thread.
     */
    fun shareSongArtworkCard(context: Context, song: SongsModel): Boolean {
        val cover = loadCover(context, song.coverUri) ?: return false
        val card = renderCard(cover, song.title, song.singer)
        val uri = runCatching {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "solo_share_${song.id}.png")
            file.outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() ?: return false

        val link = song.spotifyTrackId.takeIf { it.isNotBlank() }
            ?.let { "https://open.spotify.com/track/$it" }
            ?: "${song.title} - ${song.singer}"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, link)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Share").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
        return true
    }

    private fun loadCover(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        return runCatching {
            Glide.with(context.applicationContext)
                .asBitmap()
                .load(url)
                .submit(ART.toInt(), ART.toInt())
                .get()
        }.getOrNull()
    }

    private fun renderCard(cover: Bitmap, title: String, artist: String): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Backdrop: an azure gradient settling into the graphite canvas.
        paint.shader = LinearGradient(
            0f, 0f, W.toFloat(), H.toFloat(),
            intArrayOf(ACCENT_SOFT, SURFACE, CANVAS),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
        paint.shader = null

        // Rounded cover art near the top.
        val artTop = PAD + 40f
        val artRect = RectF(PAD, artTop, PAD + ART, artTop + ART)
        val rounded = roundBitmap(cover, ART.toInt(), ART.toInt(), 48f)
        c.drawBitmap(rounded, null, artRect, paint)

        // Title + artist under the art.
        var y = artRect.bottom + 92f
        paint.color = TEXT_PRIMARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 64f
        c.drawText(ellipsize(paint, title, ART), PAD, y, paint)

        y += 62f
        paint.color = TEXT_SECONDARY
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 44f
        c.drawText(ellipsize(paint, artist, ART), PAD, y, paint)

        // SOLO wordmark + an accent dot in the footer, with "listening on" above it.
        paint.color = TEXT_SECONDARY
        paint.textSize = 34f
        c.drawText("LISTENING ON", PAD, H - 128f, paint)

        paint.color = ACCENT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 72f
        c.drawText("SOLO", PAD, H - 60f, paint)

        // Accent play chip at the trailing footer edge.
        val chipR = 34f
        val cx = W - PAD - chipR
        val cy = H - 86f
        paint.color = ACCENT
        c.drawCircle(cx, cy, chipR, paint)
        paint.color = ON_ACCENT
        val tri = android.graphics.Path().apply {
            moveTo(cx - 11f, cy - 15f)
            lineTo(cx - 11f, cy + 15f)
            lineTo(cx + 17f, cy)
            close()
        }
        c.drawPath(tri, paint)

        return bmp
    }

    /** Centre-crops [src] to a [size] square with [radius] rounded corners. */
    private fun roundBitmap(src: Bitmap, w: Int, h: Int, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        // Centre-crop the source into the square.
        val scale = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
        val sw = w / scale
        val sh = h / scale
        val sx = (src.width - sw) / 2f
        val sy = (src.height - sh) / 2f
        val srcRect = android.graphics.Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
        canvas.drawBitmap(src, srcRect, rect, paint)
        return out
    }

    /** Trims [text] with an ellipsis so it fits within [maxWidth] at [paint]'s size. */
    private fun ellipsize(paint: Paint, text: String, maxWidth: Float): String {
        if (text.isBlank()) return text
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }
}
