package com.music.spotui.ui.screens

/**
 * First-run onboarding (FEAT-003).
 *
 * A short, three-slide premium intro shown ONCE, gated by the additive [hasOnboarded] pref:
 * a brand slide, a "free play, no login" slide, and a pick-your-accent slide that writes the
 * accent choice live. Finishing or skipping sets `hasOnboarded = true`, so existing installs
 * (flag absent) see it exactly once and never again. Styled entirely with the Graphite & Azure
 * design system and kept accessible (48dp targets, content descriptions, high-contrast text).
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.spotui.data.preferences.AccentChoice
import com.music.spotui.data.preferences.applyAccent
import com.music.spotui.data.preferences.getAccentChoice
import com.music.spotui.data.preferences.ramp
import com.music.spotui.data.preferences.setAccentChoice
import com.music.spotui.data.preferences.setOnboarded
import com.music.spotui.ui.components.SoloMark
import com.music.spotui.ui.components.SoloPillButton
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.AccentBrush
import com.music.spotui.ui.theme.AppBackgroundBrush
import com.music.spotui.ui.theme.Hairline
import com.music.spotui.ui.theme.OnAccent
import com.music.spotui.ui.theme.SoloShape
import com.music.spotui.ui.theme.Surface3
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary

/**
 * @param onFinish called when the user finishes or skips; the caller marks onboarding done
 *   and routes to Home.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }
    var accent by remember { mutableStateOf(getAccentChoice(context)) }
    val lastPage = 2

    val finish: () -> Unit = {
        setOnboarded(context, true)
        onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        // Top row: brand mark + a Skip affordance (hidden on the last page, where Finish is primary).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        ) {
            SoloMark(height = 28.dp)
            Spacer(Modifier.weight(1f))
            if (page < lastPage) {
                Text(
                    text = "Skip",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(SoloShape.pill)
                        .semantics { contentDescription = "Skip onboarding" }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .clickableNoRipple(finish),
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (fadeIn(tween(260)) togetherWith fadeOut(tween(180)))
                },
                label = "onboardingPage",
            ) { p ->
                when (p) {
                    0 -> SlideBrand()
                    1 -> SlideFreePlay()
                    else -> SlideAccent(
                        selected = accent,
                        onSelect = {
                            accent = it
                            setAccentChoice(context, it)
                            applyAccent(it)
                        },
                    )
                }
            }
        }

        // Page dots.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            repeat(lastPage + 1) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == page) 22.dp else 8.dp, 8.dp)
                        .clip(SoloShape.pill)
                        .background(if (i == page) Accent else Surface3),
                )
            }
            Spacer(Modifier.weight(1f))
        }

        SoloPillButton(
            text = if (page == lastPage) "Start listening" else "Next",
            onClick = { if (page == lastPage) finish() else page++ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
        )
    }
}

@Composable
private fun SlideBrand() {
    OnboardingSlide(
        hero = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(SoloShape.xl)
                    .background(AccentBrush),
            ) {
                SoloMark(height = 64.dp, tint = OnAccent)
            }
        },
        title = "Welcome to SOLO",
        body = "One voice. Pure sound. A premium, distraction-free player built around the music, nothing competing with it.",
    )
}

@Composable
private fun SlideFreePlay() {
    OnboardingSlide(
        hero = { HeroIcon(Icons.Rounded.MusicNote) },
        title = "Free play, no login",
        body = "Search any song, artist or album and play it instantly. No account, no sign-in, just open and listen, with high-quality smooth streaming.",
    )
}

@Composable
private fun SlideAccent(selected: AccentChoice, onSelect: (AccentChoice) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HeroIcon(Icons.Rounded.GraphicEq)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Pick your accent",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose the colour that signals through the app. You can change it any time in Settings.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(28.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.weight(1f))
            AccentChoice.entries.forEach { choice ->
                AccentSwatch(
                    choice = choice,
                    selected = choice == selected,
                    onClick = { onSelect(choice) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccentSwatch(choice: AccentChoice, selected: Boolean, onClick: () -> Unit) {
    val (_, mid, _) = choice.ramp()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(mid)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) TextPrimary else Hairline,
                    shape = CircleShape,
                )
                .semantics { contentDescription = "${choice.label} accent" }
                .clickableNoRipple(onClick),
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = OnAccent,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = choice.label,
            color = if (selected) TextPrimary else TextTertiary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun HeroIcon(icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(120.dp)
            .clip(SoloShape.xl)
            .background(Surface3)
            .border(1.dp, Hairline, SoloShape.xl),
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(56.dp))
    }
}

@Composable
private fun OnboardingSlide(
    hero: @Composable () -> Unit,
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        hero()
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/** A tap target with no ripple, used on the brand chrome where ripples would feel heavy. */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    this.clickable(interactionSource = source, indication = null, onClick = onClick)
}
