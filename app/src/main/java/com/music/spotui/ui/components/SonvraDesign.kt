package com.music.spotui.ui.components

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
import com.music.spotui.ui.theme.SonvraDisplay
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.Surface3
import com.music.spotui.ui.theme.SonvraMotion
import com.music.spotui.ui.theme.SonvraShape
import com.music.spotui.ui.theme.SonvraSpacing
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Danger
import com.music.spotui.ui.theme.DangerSurface
import androidx.compose.foundation.layout.heightIn

/**
 * Press feedback: the element eases down to 97% while held and springs back on release.
 * Pass the same [interactionSource] to the element's clickable.
 */
fun Modifier.sonvraPress(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) SonvraMotion.PRESS_SCALE else 1f,
        animationSpec = SonvraMotion.pressSpring,
        label = "sonvraPress",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Clickable with the Sonvra press-scale. [ripple] off suits artwork tiles, on suits rows. */
fun Modifier.sonvraClickable(ripple: Boolean = false, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .sonvraPress(source)
        .clickable(
            interactionSource = source,
            indication = if (ripple) LocalIndication.current else null,
            role = androidx.compose.ui.semantics.Role.Button,
            onClick = onClick,
        )
}

/** Sheet grab handle: a 32x4dp TextTertiary pill at 40%. */
@Composable
fun SonvraDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(SonvraShape.pill)
            .background(TextTertiary.copy(alpha = 0.4f)),
    )
}

/** Width-to-height ratio of drawable/logo (the tight-bounds Sonvra Sonic V mark, viewport 54 x 52). */
private const val MARK_ASPECT = 54f / 52f

/**
 * The Sonvra Sonic V mark (two capsule arms + source dot), drawn from drawable/logo so the
 * in-app mark and the launcher share one geometry. Pass [tint] for a flat version.
 */
@Composable
fun SonvraMark(
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

/** Mark + the lowercase "sonvra" wordmark set in Sora Bold. */
@Composable
fun SonvraWordmark(
    modifier: Modifier = Modifier,
    markHeight: Dp = 26.dp,
    textSize: TextUnit = 26.sp,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        SonvraMark(height = markHeight)
        Spacer(Modifier.width(9.dp))
        Text(
            text = "sonvra",
            color = TextPrimary,
            fontFamily = SonvraDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = textSize,
            letterSpacing = (-0.5).sp,
        )
    }
}

/** Section heading used by Home, Explore, Library and detail screens. */
@Composable
fun SonvraSectionHeader(
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
            .padding(start = SonvraSpacing.gutter, end = SonvraSpacing.sm, top = SonvraSpacing.xl, bottom = SonvraSpacing.sm),
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
                    .clip(SonvraShape.pill)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Loading placeholder: a Surface3 block with a soft light sweep. */
fun Modifier.shimmer(shape: Shape = SonvraShape.sm): Modifier = composed {
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
fun SonvraShimmerRow(modifier: Modifier = Modifier, artSize: Dp = 52.dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SonvraSpacing.gutter, vertical = 8.dp),
    ) {
        Box(Modifier.size(artSize).shimmer(SonvraShape.sm))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Box(Modifier.fillMaxWidth(0.62f).height(12.dp).shimmer())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.38f).height(10.dp).shimmer())
        }
    }
}

/** A column of [count] row skeletons, the standard loading state for track lists. */
@Composable
fun SonvraShimmerList(count: Int = 8, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        repeat(count) { SonvraShimmerRow() }
    }
}

/** Filter / mood chip. Selected chips are azure. */
@Composable
fun SonvraChip(
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
            .clip(SonvraShape.pill)
            .background(if (selected) Accent else Surface3)
            .border(1.dp, if (selected) Color.Transparent else Hairline, SonvraShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Pill button: accent primary or Surface3 secondary. */
@Composable
fun SonvraPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(SonvraShape.pill)
            .then(if (primary) Modifier.background(AccentBrush) else Modifier.background(Surface3))
            .border(1.dp, if (primary) Color.Transparent else Hairline, SonvraShape.pill)
            .clickable(onClick = onClick)
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
fun SonvraEmptyState(
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
                .clip(SonvraShape.md)
                .background(Surface3)
                .border(1.dp, Hairline, SonvraShape.md),
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
            SonvraPillButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Dialog confirm action: an accent-gradient pill with OnAccent text. Destructive
 * actions ([danger]) use a DangerSurface pill with Danger text instead.
 */
@Composable
fun SonvraDialogConfirm(
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(SonvraShape.pill)
            .then(if (danger) Modifier.background(DangerSurface) else Modifier.background(AccentBrush))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (danger) Danger else OnAccent)
    }
}

/** Dialog dismiss action: plain TextPrimary text with a 48dp touch target. */
@Composable
fun SonvraDialogDismiss(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(SonvraShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}

/** Round icon button with a 48dp touch target (optionally on a Surface3 well). */
@Composable
fun SonvraIconButton(
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
            .sonvraPress(source)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(Surface3) else Modifier)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}
