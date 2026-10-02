package com.music.spotui.ui.components

/**
 * Image and decorative-art primitives.
 *
 * [SoloArtwork] wraps Glide image loading with shimmer, placeholder and error states plus an
 * optional hero scrim, so every cover across the app renders consistently. `Modifier.soloMeshBackground`
 * paints a deterministic, seeded gradient mesh for decorative tiles (pure drawing, no bitmap or
 * network), used by the Explore category tiles.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.R
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.AccentDeep
import com.music.spotui.ui.theme.AccentSoft
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.ui.theme.Gold
import com.music.spotui.ui.theme.SoloShape
import com.music.spotui.ui.theme.artworkTone

/*
 * SoloArt: the image-forward design primitives shared by Home, Explore, Library and the
 * Album / Playlist / Artist detail pages.
 *
 * Two jobs:
 *  • Real cover art renders through [SoloArtwork], a thin GlideImage wrapper that reuses the
 *    app's shimmer/placeholder/error fallbacks (see AppComponents.kt) and can lay an optional
 *    contrast scrim under overlaid text.
 *  • Decorative tiles with no real artwork (mood / genre / chart cards) paint a deterministic
 *    Graphite & Azure gradient "mesh" via [Modifier.soloMeshBackground]; no bitmap, no network,
 *    so no new dependency is pulled in.
 */

/**
 * The Graphite & Azure ramp a mesh picks its stops from: soft sky -> accent -> deep -> steel.
 * A getter so the decorative mesh follows the live accent choice (FEAT-003 accent picker).
 */
private val MeshRamp: List<Color> get() = listOf(AccentSoft, Accent, AccentDeep, Gold)

/** A stable 32-bit hash of [seed] so the same label always paints the same mesh. */
private fun seedHash(seed: String): Int {
    var h = 0x811C9DC5.toInt() // FNV-1a offset basis
    for (ch in seed) {
        h = h xor ch.code
        h *= 0x01000193 // FNV prime
    }
    return h
}

/** Picks the ramp stop at [index], wrapping, so hash-derived indices always land on a colour. */
private fun ramp(index: Int): Color {
    val r = MeshRamp
    return r[index.mod(r.size)]
}

/**
 * Paints a deterministic decorative "mesh": a seed-angled, genuinely multi-stop diagonal wash
 * (seed tint -> mid accent -> graphite canvas) overlaid with two offset azure-steel radial glows
 * and finished with a faint diagonal sheen, so each tile reads as its own crafted premium surface
 * while staying on the Graphite & Azure palette. [seed] (usually the tile's label) picks the
 * stops, the wash angle and the glow anchors, so adjacent tiles look clearly distinct and the art
 * is stable across recompositions and process restarts.
 *
 * Guaranteed to look premium fully OFFLINE: pure drawing, three-stop base (never a flat two-tone
 * box), varied hue and angle per label. No bitmap decode and no network, so it adds no dependency.
 */
fun Modifier.soloMeshBackground(seed: String, shape: Shape = SoloShape.md): Modifier = composed {
    val hash = remember(seed) { seedHash(seed) }
    // Pick three visibly different ramp stops using well-separated hash bit windows and an
    // odd stride, so neighbouring labels rarely collide on the same accent pairing.
    val step = ((hash ushr 7) and 0x3) + 1 // 1..4 stride through the ramp
    val baseIdx = (hash ushr 9).mod(MeshRamp.size)
    val accent = ramp(baseIdx + step)       // primary glow
    val accent2 = ramp(baseIdx + step * 2)  // secondary glow, a different stop
    // artworkTone keeps the base dark enough that light labels stay legible on top.
    val base = artworkTone(ramp(baseIdx))
    // A mid accent for the base wash so it is a true 3+ stop gradient, not a flat two-color box.
    val mid = ramp(baseIdx + step).copy(alpha = 0.55f)
    // Seed the base wash ANGLE from the hash so the diagonal is not always top-left -> bottom-right.
    val angleBucket = (hash ushr 5) and 0x3 // 0..3 -> four distinct diagonal directions
    val anchorX = 0.18f + ((hash ushr 2) and 0xFF) / 255f * 0.64f
    val anchorY = 0.12f + ((hash ushr 11) and 0xFF) / 255f * 0.5f
    // Second glow sits opposite-ish the first for depth, nudged by its own hash bits.
    val anchor2X = 0.9f - ((hash ushr 19) and 0xFF) / 255f * 0.6f
    val anchor2Y = 0.95f - ((hash ushr 23) and 0xFF) / 255f * 0.55f
    // A faint diagonal sheen angle, so no two tiles share the same highlight direction.
    val sheenDown = (hash ushr 1) and 1 == 0

    this
        .clip(shape)
        .drawBehind {
            val w = size.width
            val h = size.height
            // Base wash start/end chosen from the seeded angle bucket so the direction varies.
            val (washStart, washEnd) = when (angleBucket) {
                0 -> Offset(0f, 0f) to Offset(w, h)       // TL -> BR
                1 -> Offset(w, 0f) to Offset(0f, h)       // TR -> BL
                2 -> Offset(0f, h * 0.2f) to Offset(w, h) // left-ish down
                else -> Offset(w * 0.5f, 0f) to Offset(w * 0.5f, h) // top -> bottom
            }
            // Base diagonal wash: a true multi-stop gradient (seed tint -> mid accent -> canvas),
            // so even with no cover loaded the tile has depth rather than a flat two-color fill.
            drawRect(
                Brush.linearGradient(
                    0f to base,
                    0.55f to mid,
                    1f to Canvas,
                    start = washStart,
                    end = washEnd,
                ),
            )
            // Primary azure glow anchored at the seeded point, fading out before the edges.
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.52f), accent.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(w * anchorX, h * anchorY),
                    radius = size.maxDimension * 0.78f,
                ),
            )
            // Secondary, cooler/warmer glow offset for depth and variety.
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent2.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(w * anchor2X, h * anchor2Y),
                    radius = size.maxDimension * 0.6f,
                ),
            )
            // Faint diagonal sheen for a crafted, non-flat finish.
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent, Color.Black.copy(alpha = 0.12f)),
                    start = if (sheenDown) Offset(0f, 0f) else Offset(w, 0f),
                    end = if (sheenDown) Offset(w, h) else Offset(0f, h),
                ),
            )
        }
}

/**
 * A top-to-bottom scrim brush for hero surfaces: transparent at the top, deepening to a near
 * opaque canvas at the bottom so a title and metadata laid over artwork stay legible.
 */
val SoloHeroScrim: Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    0.45f to Canvas.copy(alpha = 0.15f),
    0.78f to Canvas.copy(alpha = 0.62f),
    1f to Canvas.copy(alpha = 0.92f),
)

/**
 * Renders real cover art through Glide with the app's standard shimmer/placeholder/error
 * fallbacks (mirrors the GlideImage pattern in AppComponents.kt). Pass [scrim] = true to lay
 * [SoloHeroScrim] under overlaid text for contrast.
 *
 * Set [meshFallback] = true for tiles that already paint a [Modifier.soloMeshBackground] base
 * (the Explore category tiles): while the photo loads a shimmer sweeps over the mesh, and on a
 * decode failure the artwork stays fully transparent so the premium mesh shows through, instead
 * of the opaque flat [R.drawable.placeholder] covering it with a gray box.
 *
 * The caller supplies [modifier] with the size; [shape] clips the whole tile.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SoloArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = SoloShape.md,
    scrim: Boolean = false,
    meshFallback: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    Box(modifier = modifier.clip(shape)) {
        GlideImage(
            modifier = Modifier.fillMaxSize(),
            model = model,
            contentScale = contentScale,
            // On tiles with a mesh base, draw a shimmer sweep (over the mesh) while loading and
            // leave the surface transparent on a decode failure so the premium mesh shows through;
            // everywhere else keep the app's flat fallback drawable.
            failure = if (meshFallback) placeholder { } else placeholder(R.drawable.placeholder),
            loading = if (meshFallback) {
                placeholder { Box(modifier = Modifier.fillMaxSize().shimmer(shape)) }
            } else {
                placeholder(R.drawable.placeholder)
            },
            contentDescription = contentDescription,
        )
        if (scrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoloHeroScrim),
            )
        }
    }
}
