package com.music.spotui.ui.components

import androidx.compose.animation.core.LinearEasing
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

/** Width-to-height ratio of drawable/logo (the tight-bounds Solo mark). */
private const val MARK_ASPECT = 25.6f / 56f

/** The Solo "Pure Tone" mark. Pass [tint] for a flat single-colour version. */
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

/** Mark + "Solo" set in the display face. */
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
            text = "Solo",
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

/** Round icon button with a 40dp touch target (optionally on a velvet well). */
@Composable
fun SoloIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ivory,
    filled: Boolean = false,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(Velvet) else Modifier)
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}
