package com.music.spotui.ui.screens

/**
 * Listening stats / recap (FEAT-003) — READ-ONLY.
 *
 * Renders the on-device taste model as a premium recap: top artists and tracks, total listens,
 * estimated minutes and a day streak. It consumes [TasteProfile.listeningSummary] only (a
 * read-only aggregate of the existing per-track counters); it never writes, never changes
 * `recordOutcome`'s signature, and never touches the TasteProfile prefs-name / JSON keys.
 * Styled with the Graphite & Azure design system, with an empty state before any data exists.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.spotui.data.recommendation.TasteProfile
import com.music.spotui.ui.components.SoloEmptyState
import com.music.spotui.ui.components.SoloIconButton
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.AccentBrush
import com.music.spotui.ui.theme.AppBackgroundBrush
import com.music.spotui.ui.theme.Elevated
import com.music.spotui.ui.theme.Hairline
import com.music.spotui.ui.theme.HairlineAccent
import com.music.spotui.ui.theme.OnAccent
import com.music.spotui.ui.theme.SoloShape
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.Surface3
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary

@Composable
fun ListeningStatsScreen(navController: NavController) {
    val context = LocalContext.current
    // Derived off the main thread: snapshotting + sorting the taste map shouldn't block the frame.
    val summary by produceState<TasteProfile.ListeningSummary?>(initialValue = null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            TasteProfile.listeningSummary(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            SoloIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = { navController.popBackStack() },
            )
            Text(
                "Your listening",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        val s = summary
        when {
            s == null -> Unit // brief loading gap; the aggregate is near-instant
            s.totalListens == 0 && s.trackCount == 0 -> SoloEmptyState(
                icon = Icons.Rounded.BarChart,
                title = "No listening yet",
                message = "Play a few songs and your top artists, tracks and listening streak will show up here.",
                modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
            )
            else -> StatsContent(s)
        }
    }
}

@Composable
private fun StatsContent(s: TasteProfile.ListeningSummary) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp, bottom = 200.dp,
        ),
    ) {
        item {
            // Hero recap tile.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoloShape.lg)
                    .background(Brush.verticalGradient(listOf(Elevated, Surface2)))
                    .border(1.dp, HairlineAccent, SoloShape.lg)
                    .padding(20.dp),
            ) {
                Text(
                    "${s.estimatedMinutes} min",
                    color = TextPrimary,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "estimated listening across ${s.trackCount} tracks",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatPill("Listens", s.totalListens.toString(), Modifier.weight(1f))
                    StatPill("Artists", s.artistCount.toString(), Modifier.weight(1f))
                    StatPill("Day streak", s.dayStreak.toString(), Modifier.weight(1f))
                }
                s.strongestCluster?.let { cluster ->
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "You're leaning ${cluster.replaceFirstChar { it.uppercase() }} right now",
                        color = Accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (s.topArtists.isNotEmpty()) {
            item { SectionLabel("TOP ARTISTS") }
            itemsIndexed(s.topArtists) { index, artist ->
                RankRow(rank = index + 1, title = artist, subtitle = null)
            }
        }

        if (s.topTracks.isNotEmpty()) {
            item { SectionLabel("TOP TRACKS") }
            itemsIndexed(s.topTracks) { index, (title, artist) ->
                RankRow(rank = index + 1, title = title, subtitle = artist.takeIf { it.isNotBlank() })
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Built on-device from your plays. Nothing here leaves your phone.",
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(SoloShape.md)
            .background(Surface3)
            .border(1.dp, Hairline, SoloShape.md)
            .padding(vertical = 12.dp, horizontal = 10.dp),
    ) {
        Text(value, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Accent,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun RankRow(rank: Int, title: String, subtitle: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (rank == 1) AccentBrush else Brush.linearGradient(listOf(Surface3, Surface3))),
        ) {
            Text(
                "$rank",
                color = if (rank == 1) OnAccent else TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(0.dp))
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
