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
 *    Volt-family gradient "mesh" via [Modifier.soloMeshBackground]; no bitmap, no network, so
 *    no new dependency is pulled in.
 */

/** The Volt accent ramp a mesh picks its stops from, biased toward the premium lime-citron family. */
private val VoltMeshRamp = listOf(AccentSoft, Accent, AccentDeep)

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
 * Paints a deterministic decorative "mesh": a Volt-family radial glow blended over a
 * seed-tinted linear gradient that settles into the obsidian canvas, so a tile reads as its
 * own colourful surface while staying on the Aurora Noir palette. [seed] (usually the tile's
 * label) picks the stops, so the art is stable across recompositions and process restarts.
 *
 * Pure drawing — no bitmap decode and no network — so decorative art adds no dependency.
 */
fun Modifier.soloMeshBackground(seed: String, shape: Shape = SoloShape.md): Modifier = composed {
    val hash = remember(seed) { seedHash(seed) }
    // Derive the accent stop and the glow anchor deterministically from the hash.
    val accent = VoltMeshRamp[(hash ushr 3).mod(VoltMeshRamp.size)]
    // artworkTone keeps the base dark enough that light labels stay legible on top.
    val base = artworkTone(VoltMeshRamp[(hash ushr 9).mod(VoltMeshRamp.size)])
    val anchorX = 0.2f + ((hash ushr 2) and 0xFF) / 255f * 0.6f
    val anchorY = 0.15f + ((hash ushr 11) and 0xFF) / 255f * 0.45f

    this
        .clip(shape)
        .drawBehind {
            // Base linear wash: a seed-tinted top settling into the canvas.
            drawRect(
                Brush.linearGradient(
                    colors = listOf(base, Canvas),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
            // Radial Volt glow anchored at the seeded point, fading out well before the edges.
            val cx = size.width * anchorX
            val cy = size.height * anchorY
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.maxDimension * 0.75f,
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
