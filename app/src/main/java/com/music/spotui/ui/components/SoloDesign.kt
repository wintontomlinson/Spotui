package com.music.spotui.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.R
import com.music.spotui.ui.theme.Amethyst
import com.music.spotui.ui.theme.Gold
import com.music.spotui.ui.theme.GoldBrush
import com.music.spotui.ui.theme.HairlineSoft
import com.music.spotui.ui.theme.Ivory
import com.music.spotui.ui.theme.OnGold
import com.music.spotui.ui.theme.SoloDisplay
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.Velvet

/** Shared motion tokens so every surface moves with the same rhythm. */
object SoloMotion {
    const val PRESS_SCALE = 0.96f
    const val FADE_MS = 220
    const val NAV_MS = 180
    const val EXPAND_MS = 320
    const val ICON_MS = 150
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val pressSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 600f)
    fun <T> fade() = tween<T>(durationMillis = FADE_MS, easing = FastOutSlowInEasing)
}

/**
 * Press feedback: the element eases down to 96% while held and springs back on release.
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

/** Sheet grab handle: a 36x4dp soft ivory pill. */
@Composable
fun SoloDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(Ivory.copy(alpha = 0.22f)),
    )
}

/** Width-to-height ratio of drawable/logo (the tight-bounds Sonvra Sonic V mark, viewport 54 x 52). */
private const val MARK_ASPECT = 54f / 52f

/**
 * The Sonvra Sonic V mark (two capsule arms + source dot), drawn from drawable/logo so the
 * in-app mark and the launcher share one geometry. Pass [tint] for a flat version.
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

/** Mark + the lowercase "sonvra" wordmark set in the display face. */
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
            text = "sonvra",
            color = Gold,
            fontFamily = SoloDisplay,
            fontSize = textSize,
            letterSpacing = (-0.3).sp,
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
            .padding(start = 18.dp, end = 12.dp, top = 24.dp, bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Ivory,
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
                color = Gold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Loading placeholder: a velvet block with a soft light sweep. */
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier = composed {
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
            drawRect(Velvet)
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, Amethyst, Color.Transparent),
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
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(artSize).shimmer(RoundedCornerShape(10.dp)))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Box(Modifier.fillMaxWidth(0.62f).height(12.dp).shimmer())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.38f).height(10.dp).shimmer())
        }
    }
}

/** Skeleton for a square artwork card with a caption. */
@Composable
fun SoloShimmerCard(modifier: Modifier = Modifier, size: Dp = 156.dp) {
    Column(modifier = modifier.width(size)) {
        Box(Modifier.size(size).shimmer(RoundedCornerShape(16.dp)))
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth(0.85f).height(12.dp).shimmer())
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth(0.55f).height(10.dp).shimmer())
    }
}

/** A column of [count] row skeletons, the standard loading state for track lists. */
@Composable
fun SoloShimmerList(count: Int = 8, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        repeat(count) { SoloShimmerRow() }
    }
}

/** Filter / mood chip. Selected chips are gold. */
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
        color = if (selected) OnGold else Ivory,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Gold else Velvet)
            .border(1.dp, if (selected) Color.Transparent else HairlineSoft, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Pill button: gold primary or velvet secondary. */
@Composable
fun SoloPillButton(
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
            .clip(RoundedCornerShape(50))
            .then(if (primary) Modifier.background(GoldBrush) else Modifier.background(Velvet))
            .border(1.dp, if (primary) Color.Transparent else HairlineSoft, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (primary) OnGold else Ivory, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (primary) OnGold else Ivory)
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
                .size(68.dp)
                .clip(CircleShape)
                .background(Velvet)
                .border(1.dp, HairlineSoft, CircleShape),
        ) {
            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Ivory,
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

/** Round icon button with a 48dp touch target (optionally on a velvet well). */
@Composable
fun SoloIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ivory,
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
            .then(if (filled) Modifier.background(Velvet) else Modifier)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}
