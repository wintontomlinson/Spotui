package com.music.spotui.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.SonvraChip
import com.music.spotui.ui.components.SonvraEmptyState
import com.music.spotui.ui.components.SonvraIconButton
import com.music.spotui.ui.theme.AppBackgroundBrush
import com.music.spotui.ui.theme.AccentBrush
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.Hairline
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.OnAccent
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3
import kotlin.math.roundToInt

/**
 * Equalizer: an enable switch, preset chips and one vertical slider per device band
 * (Android's [android.media.audiofx.Equalizer] on the playback session). Changes are
 * applied live and saved, so they survive a restart.
 */
@Composable
fun EqualizerScreen(navController: NavController) {
    val context = LocalContext.current
    val supported = remember { SongPlayer.ensureEqualizer(context) }
    var enabled by remember { mutableStateOf(com.music.spotui.data.preferences.isEqEnabled(context)) }
    var preset by remember { mutableStateOf(com.music.spotui.data.preferences.getEqPreset(context)) }
    var levels by remember { mutableStateOf(SongPlayer.eqBandLevels()) }
    val freqs = remember { SongPlayer.eqCenterFreqs() }
    val range = remember { SongPlayer.eqBandRange() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 160.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            SonvraIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = { navController.popBackStack() },
            )
            Text(
                "Equalizer",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (supported) {
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        SongPlayer.setEqEnabled(context, it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnAccent,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Surface3,
                    ),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .semantics { contentDescription = "Equalizer on" },
                )
            }
        }

        if (!supported || levels.isEmpty()) {
            SonvraEmptyState(
                icon = Icons.Rounded.GraphicEq,
                title = "Equalizer isn't supported on this device",
                message = "Your phone doesn't expose an audio equalizer to apps.",
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            )
            return@Column
        }

        Text(
            "PRESETS",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 10.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            (SongPlayer.EQ_PRESETS.map { it.first } + if (preset == "Custom") listOf("Custom") else emptyList())
                .forEach { name ->
                    SonvraChip(
                        label = name,
                        selected = preset == name,
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = {
                            if (name == "Custom") return@SonvraChip
                            preset = name
                            SongPlayer.applyEqPreset(context, name)
                            levels = SongPlayer.eqBandLevels()
                            if (!enabled) {
                                enabled = true
                                SongPlayer.setEqEnabled(context, true)
                            }
                        },
                    )
                }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .padding(16.dp)
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface2)
                .border(1.dp, Hairline, RoundedCornerShape(24.dp))
                .padding(vertical = 20.dp, horizontal = 6.dp),
        ) {
            levels.forEachIndexed { band, level ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDb(level),
                        color = if (enabled) Accent else TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    VerticalBandSlider(
                        value = level,
                        min = range.first,
                        max = range.second,
                        active = enabled,
                        label = "${formatHz(freqs.getOrNull(band) ?: 0)} band",
                        onChange = { mB ->
                            levels = levels.toMutableList().also { it[band] = mB }
                            preset = "Custom"
                        },
                        onChangeFinished = { SongPlayer.setEqBand(context, band, levels[band]) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(formatHz(freqs.getOrNull(band) ?: 0), color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
        Text(
            "Changes apply instantly and are kept for every song.",
            color = TextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

private fun formatHz(hz: Int): String =
    if (hz >= 1000) {
        val k = hz / 1000f
        (if (k >= 10f || k == k.toInt().toFloat()) "${k.roundToInt()}" else "%.1f".format(k)) + " kHz"
    } else "$hz Hz"

private fun formatDb(mB: Int): String {
    val db = mB / 100f
    val txt = if (db == db.toInt().toFloat()) db.toInt().toString() else "%.1f".format(db)
    return if (db > 0) "+$txt" else txt
}

/** A 48dp-wide vertical slider: accent fill from 0 dB toward the thumb. */
@Composable
private fun VerticalBandSlider(
    value: Int,
    min: Int,
    max: Int,
    active: Boolean,
    label: String,
    onChange: (Int) -> Unit,
    onChangeFinished: () -> Unit,
) {
    val span = (max - min).coerceAtLeast(1)
    fun fromY(y: Float, h: Float): Int {
        val frac = 1f - (y / h).coerceIn(0f, 1f)
        // Snap to 0.5 dB steps.
        return ((min + frac * span) / 50f).roundToInt() * 50
    }
    Canvas(
        modifier = Modifier
            .width(48.dp)
            .height(200.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), min.toFloat()..max.toFloat())
                setProgress { target ->
                    onChange(target.roundToInt().coerceIn(min, max)); onChangeFinished(); true
                }
            }
            .pointerInput(min, max) {
                detectVerticalDragGestures(
                    onDragEnd = onChangeFinished,
                    onDragCancel = onChangeFinished,
                ) { change, _ ->
                    change.consume()
                    onChange(fromY(change.position.y, size.height.toFloat()).coerceIn(min, max))
                }
            }
            .pointerInput(min, max) {
                detectTapGestures { pos ->
                    onChange(fromY(pos.y, size.height.toFloat()).coerceIn(min, max))
                    onChangeFinished()
                }
            },
    ) {
        val trackW = 4.dp.toPx()
        val cx = size.width / 2f
        val h = size.height
        fun yOf(v: Int) = h * (1f - (v - min).toFloat() / span)
        drawRoundRect(
            color = Surface3,
            topLeft = Offset(cx - trackW / 2, 0f),
            size = Size(trackW, h),
            cornerRadius = CornerRadius(trackW / 2),
        )
        val zeroY = yOf(0.coerceIn(min, max))
        val y = yOf(value)
        val top = minOf(zeroY, y)
        val bottom = maxOf(zeroY, y)
        if (active) {
            drawRoundRect(
                brush = AccentBrush,
                topLeft = Offset(cx - trackW / 2, top),
                size = Size(trackW, (bottom - top).coerceAtLeast(1f)),
                cornerRadius = CornerRadius(trackW / 2),
            )
        }
        drawCircle(color = if (active) TextPrimary else TextTertiary, radius = 9.dp.toPx(), center = Offset(cx, y))
    }
}
