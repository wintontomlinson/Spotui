package com.music.spotui.ui.components

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
 *    Lumen-prism gradient "mesh" via [Modifier.soloMeshBackground]; no bitmap, no network, so
 *    no new dependency is pulled in.
 */

/** The Lumen prism ramp a mesh picks its stops from: violet -> cyan -> gold. */
private val LumenMeshRamp = listOf(AccentSoft, Accent, AccentDeep, Gold)

/** A stable 32-bit hash of [seed] so the same label always paints the same mesh. */
private fun seedHash(seed: String): Int {
    var h = 0x811C9DC5.toInt() // FNV-1a offset basis
    for (ch in seed) {
        h = h xor ch.code
        h *= 0x01000193 // FNV prime
    }
    return h
}

/**
 * Paints a deterministic decorative "mesh": two offset prism-coloured radial glows blended
 * over a seed-tinted diagonal gradient that settles into the indigo canvas, finished with a
 * faint diagonal sheen so each tile reads as its own crafted surface while staying on the
 * Lumen Indigo palette. [seed] (usually the tile's label) picks the stops and anchors, so the
 * art is stable across recompositions and process restarts.
 *
 * Pure drawing — no bitmap decode and no network — so decorative art adds no dependency.
 */
fun Modifier.soloMeshBackground(seed: String, shape: Shape = SoloShape.md): Modifier = composed {
    val hash = remember(seed) { seedHash(seed) }
    // Derive two distinct accent stops and both glow anchors deterministically from the hash.
    val accent = LumenMeshRamp[(hash ushr 3).mod(LumenMeshRamp.size)]
    val accent2 = LumenMeshRamp[(hash ushr 15).mod(LumenMeshRamp.size)]
    // artworkTone keeps the base dark enough that light labels stay legible on top.
    val base = artworkTone(LumenMeshRamp[(hash ushr 9).mod(LumenMeshRamp.size)])
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
            // Base diagonal wash: a seed-tinted corner settling into the indigo canvas.
            drawRect(
                Brush.linearGradient(
                    colors = listOf(base, Canvas),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
            // Primary prism glow anchored at the seeded point, fading out before the edges.
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.52f), accent.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width * anchorX, size.height * anchorY),
                    radius = size.maxDimension * 0.78f,
                ),
            )
            // Secondary, cooler/warmer glow offset for depth and variety.
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent2.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * anchor2X, size.height * anchor2Y),
                    radius = size.maxDimension * 0.6f,
                ),
            )
            // Faint diagonal sheen for a crafted, non-flat finish.
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent, Color.Black.copy(alpha = 0.12f)),
                    start = if (sheenDown) Offset(0f, 0f) else Offset(size.width, 0f),
                    end = if (sheenDown) Offset(size.width, size.height) else Offset(0f, size.height),
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
 * The caller supplies [modifier] with the size; [shape] clips the whole tile.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SoloArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = SoloShape.md,
    scrim: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    Box(modifier = modifier.clip(shape)) {
        GlideImage(
            modifier = Modifier.fillMaxSize(),
            model = model,
            contentScale = contentScale,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
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
