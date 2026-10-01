package com.music.spotui.ui.components

/**
 * Core SOLO design primitives shared across screens: the brand [SoloMark] / [SoloWordmark], the
 * [SoloPillButton], and reusable [Modifier] helpers — `shimmer()`, `soloPress()`,
 * `soloClickable()` and `soloGlass()` (API 31+ `RenderEffect` blur with an opaque fallback below).
 * Keeping these here means buttons, loading states and glass surfaces look and behave the same
 * everywhere.
 */

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.R
import com.music.spotui.ui.theme.Surface4
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.AccentBrush
import com.music.spotui.ui.theme.Hairline
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.OnAccent
import com.music.spotui.ui.theme.SoloDisplay
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.Surface3
import com.music.spotui.ui.theme.SoloMotion
import com.music.spotui.ui.theme.SoloShape
import com.music.spotui.ui.theme.SoloSpacing
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Danger
import com.music.spotui.ui.theme.DangerSurface
import androidx.compose.foundation.layout.heightIn

/**
 * Premium frosted-glass chrome. On API 31+ the surface behind this element is blurred with a
 * [RenderEffect] and tinted with a translucent [fill]; below 31 (no RenderEffect) it falls back
 * to the opaque [fallback] tint so the chrome stays legible. Mirrors the API-31 blur guard used
 * on the player backdrop. Place this before the content/padding modifiers.
 */
fun Modifier.soloGlass(
    fill: Color,
    fallback: Color,
    blurRadius: Dp = 24.dp,
    shape: Shape = androidx.compose.ui.graphics.RectangleShape,
): Modifier = composed {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val radiusPx = with(density) { blurRadius.toPx() }
        this
            .clip(shape)
            .graphicsLayer {
                // A clamped blur of whatever renders behind this layer, for a frosted backdrop.
                renderEffect = android.graphics.RenderEffect
                    .createBlurEffect(radiusPx, radiusPx, android.graphics.Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
            .background(fill)
    } else {
        this
            .clip(shape)
            .background(fallback)
    }
}

/**
 * Press feedback: the element eases down to 97% while held and springs back on release.
 * Pass the same [interactionSource] to the element's clickable.
 */
fun Modifier.soloPress(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) SoloMotion.PRESS_SCALE else 1f,
        animationSpec = SoloMotion.pressSpring,
        label = "soloPress",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Clickable with the Solo press-scale. [ripple] off suits artwork tiles, on suits rows. */
fun Modifier.soloClickable(ripple: Boolean = false, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .soloPress(source)
        .clickable(
            interactionSource = source,
            indication = if (ripple) LocalIndication.current else null,
            role = androidx.compose.ui.semantics.Role.Button,
            onClick = onClick,
        )
}

/** Sheet grab handle: a 32x4dp TextTertiary pill at 40%. */
@Composable
fun SoloDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(SoloShape.pill)
            .background(TextTertiary.copy(alpha = 0.4f)),
    )
}

/** Width-to-height ratio of drawable/logo (the tight-bounds Lumen Prism mark, viewport 62 x 62). */
private const val MARK_ASPECT = 62f / 62f

/**
 * The SOLO Lumen Prism mark (a gold-tipped violet beam fanning into a violet->cyan spectrum),
 * drawn from drawable/logo so the in-app mark and the launcher share one geometry. Pass [tint]
 * for a flat version.
 */
@Composable
fun SoloMark(
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
    tint: Color? = null,
) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier.size(width = height * MARK_ASPECT, height = height),
    )
}

/** Mark + the "SOLO" wordmark set in Space Grotesk Bold, all caps. */
@Composable
fun SoloWordmark(
    modifier: Modifier = Modifier,
    markHeight: Dp = 26.dp,
    textSize: TextUnit = 26.sp,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        SoloMark(height = markHeight)
        Spacer(Modifier.width(9.dp))
        Text(
            text = "SOLO",
            color = TextPrimary,
            fontFamily = SoloDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = textSize,
            letterSpacing = 0.5.sp,
        )
    }
}

/** Section heading used by Home, Explore, Library and detail screens. */
@Composable
fun SoloSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SoloSpacing.gutter, end = SoloSpacing.sm, top = SoloSpacing.xl, bottom = SoloSpacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                maxLines = 1,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                modifier = Modifier
                    .clip(SoloShape.pill)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Loading placeholder: a Surface3 block with a soft light sweep. */
fun Modifier.shimmer(shape: Shape = SoloShape.sm): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    this
        .clip(shape)
        .drawBehind {
            val w = size.width.coerceAtLeast(1f)
            val x = -w + 3f * w * progress
            drawRect(Surface3)
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, Surface4, Color.Transparent),
                    start = Offset(x, 0f),
                    end = Offset(x + w, size.height),
                ),
            )
        }
}

/** Skeleton for a list row: square art plus two text lines. */
@Composable
fun SoloShimmerRow(modifier: Modifier = Modifier, artSize: Dp = 52.dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SoloSpacing.gutter, vertical = 8.dp),
    ) {
        Box(Modifier.size(artSize).shimmer(SoloShape.sm))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Box(Modifier.fillMaxWidth(0.62f).height(12.dp).shimmer())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.38f).height(10.dp).shimmer())
        }
    }
}

/** A column of [count] row skeletons, the standard loading state for track lists. */
@Composable
fun SoloShimmerList(count: Int = 8, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        repeat(count) { SoloShimmerRow() }
    }
}

/** Filter / mood chip. Selected chips are Lumen violet. */
@Composable
fun SoloChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) OnAccent else TextPrimary,
        modifier = modifier
            .clip(SoloShape.pill)
            .background(if (selected) Accent else Surface3)
            .border(1.dp, if (selected) Color.Transparent else Hairline, SoloShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Pill button: accent primary or Surface3 secondary. */
@Composable
fun SoloPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = true,
) {
    // A light tactile tick on the primary CTA so it feels premium under the thumb.
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(SoloShape.pill)
            .then(if (primary) Modifier.background(AccentBrush) else Modifier.background(Surface3))
            .border(1.dp, if (primary) Color.Transparent else Hairline, SoloShape.pill)
            .clickable {
                if (primary) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (primary) OnAccent else TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (primary) OnAccent else TextPrimary)
    }
}

/** Centered empty / error message with an optional action. */
@Composable
fun SoloEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(SoloShape.md)
                .background(Surface3)
                .border(1.dp, Hairline, SoloShape.md),
        ) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            SoloPillButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Dialog confirm action: an accent-gradient pill with OnAccent text. Destructive
 * actions ([danger]) use a DangerSurface pill with Danger text instead.
 */
@Composable
fun SoloDialogConfirm(
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(SoloShape.pill)
            .then(if (danger) Modifier.background(DangerSurface) else Modifier.background(AccentBrush))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (danger) Danger else OnAccent)
    }
}

/** Dialog dismiss action: plain TextPrimary text with a 48dp touch target. */
@Composable
fun SoloDialogDismiss(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(SoloShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}

/** Round icon button with a 48dp touch target (optionally on a Surface3 well). */
@Composable
fun SoloIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    filled: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .soloPress(source)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(Surface3) else Modifier)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}
