package com.music.spotui.ui.screens

/**
 * Settings tab (`SettingsScreen`).
 *
 * Groups the app's preferences — playback/audio, appearance, backup, updates — and hosts the
 * compact [AboutCard] (mark, name + version, tagline, maintainer/credits/disclaimer) rather than
 * a separate About screen. Reads and writes through the settings preferences store.
 */

import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.music.spotui.R
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import com.music.spotui.data.preferences.BackupPref
import com.music.spotui.util.BackupHelper
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
import com.music.spotui.data.preferences.AudioProviderOrderItem
import com.music.spotui.data.preferences.getAudioProviderOrder
import com.music.spotui.data.preferences.setAudioProviderOrder
import com.music.spotui.data.preferences.isAudioProviderEnabled
import com.music.spotui.data.preferences.setAudioProviderEnabled
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.music.spotui.data.BatteryOptimizationHelper
import com.music.spotui.data.preferences.CROSSFADE_MAX_MS
import com.music.spotui.data.preferences.StreamQuality
import com.music.spotui.data.preferences.getCellularQuality
import com.music.spotui.data.preferences.getCrossfadeMs
import com.music.spotui.data.preferences.setCrossfadeMs
import com.music.spotui.data.preferences.getDownloadQuality
import com.music.spotui.data.preferences.isVideoFallbackEnabled
import com.music.spotui.data.preferences.isAutoPlayEnabled
import com.music.spotui.data.preferences.getWifiQuality
import com.music.spotui.data.preferences.setCellularQuality
import com.music.spotui.data.preferences.setDownloadQuality
import com.music.spotui.data.preferences.setAutoPlayEnabled
import com.music.spotui.data.preferences.setVideoFallbackEnabled
import com.music.spotui.data.preferences.setWifiQuality
import com.music.spotui.data.preferences.getUpdateRepoUrl
import com.music.spotui.data.preferences.setUpdateRepoUrl
import com.music.spotui.data.preferences.resetUpdateRepoUrl
import com.music.spotui.data.preferences.DEFAULT_UPDATE_REPO_URL
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.data.diagnostics.PlaybackLog
import com.music.spotui.ui.theme.Accent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.heightIn
import com.music.spotui.ui.theme.Surface4
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.OnAccent
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    var wifiQ by remember { mutableStateOf(getWifiQuality(context)) }
    var cellQ by remember { mutableStateOf(getCellularQuality(context)) }
    var dlQ by remember { mutableStateOf(getDownloadQuality(context)) }
    var crossfadeMs by remember { mutableStateOf(getCrossfadeMs(context).toFloat()) }
    var videoFallback by remember { mutableStateOf(isVideoFallbackEnabled(context)) }
    var autoPlay by remember { mutableStateOf(isAutoPlayEnabled(context)) }
    var normalize by remember { mutableStateOf(com.music.spotui.data.preferences.isNormalizeVolume(context)) }
    var batteryOptExempt by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimization(context)) }

    var backupDirUri by remember { mutableStateOf(BackupPref.getDirectoryUri(context)) }
    var folderName by remember(backupDirUri) { mutableStateOf(BackupHelper.getFolderDisplayName(context, backupDirUri)) }
    var isAutoBackup by remember { mutableStateOf(BackupPref.isAutoBackupEnabled(context)) }
    var isRestoring by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var showPlaybackLog by remember { mutableStateOf(false) }

    if (showPlaybackLog) {
        PlaybackLogDialog(onDismiss = { showPlaybackLog = false })
    }
    val scope = rememberCoroutineScope()

    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            BackupPref.setDirectoryUri(context, uri.toString())
            backupDirUri = uri.toString()
            folderName = BackupHelper.getFolderDisplayName(context, uri.toString())
            scope.launch {
                val autoOk = BackupHelper.performAutoBackup(context)
                val msg = if (autoOk) "Backup folder set to $folderName (Auto-backup created)" else "Backup folder set to $folderName"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isRestoring = true
            scope.launch {
                val (success, message) = BackupHelper.restoreFromFileUri(context, uri)
                isRestoring = false
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                if (success) {
                    wifiQ = getWifiQuality(context)
                    cellQ = getCellularQuality(context)
                    dlQ = getDownloadQuality(context)
                    crossfadeMs = getCrossfadeMs(context).toFloat()
                    videoFallback = isVideoFallbackEnabled(context)
                    autoPlay = isAutoPlayEnabled(context)
                    backupDirUri = BackupPref.getDirectoryUri(context)
                    isAutoBackup = BackupPref.isAutoBackupEnabled(context)
                }
            }
        }
    }

    val batteryOptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        batteryOptExempt = BatteryOptimizationHelper.isIgnoringBatteryOptimization(context)
    }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = TextPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    com.music.spotui.ui.components.SoloIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { navController.popBackStack() },
                        filled = true,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Canvas)
            )
        }
    ) { padding ->
        var showDevicesSheet by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                // Clear the bottom nav + mini player so the last section
                // (account / log out) isn't hidden under the bar.
                .padding(bottom = 200.dp)
        ) {
            SectionTitle("Devices & Bluetooth")
            SettingsClickRow(
                title = "Audio Output Devices",
                subtitle = com.music.spotui.ui.utils.AudioDeviceHelper.getCurrentAudioRouteName(context),
                subtitleColor = SettingsAccent,
                leadingIcon = {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Devices),
                        contentDescription = null,
                        tint = SettingsAccent,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = SettingsTextDim,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = { showDevicesSheet = true },
            )

            Spacer(Modifier.height(8.dp))

            SectionTitle("Background playback")
            SettingsClickRow(
                title = "Battery optimization",
                subtitle = if (batteryOptExempt) "Exempt, app won't be killed" else "Not exempt, tap to change",
                subtitleColor = if (batteryOptExempt) com.music.spotui.ui.theme.Success else SettingsTextDim,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = SettingsAccent,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailing = if (batteryOptExempt) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Enabled",
                            tint = SettingsAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else null,
                onClick = {
                    batteryOptLauncher.launch(BatteryOptimizationHelper.buildAppSettingsIntent(context))
                },
            )
            BatteryOptimizationHelper.getManufacturerTips()?.let { (name, tip) ->
                Text(
                    text = "Tip for $name",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Text(
                    text = tip,
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoloShape.md)
                        .background(SettingsCard)
                        .border(1.dp, SettingsHairline, SoloShape.md)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(8.dp))
            }


            SectionTitle("Audio quality")
            QualityPicker(
                title = "Streaming over Wi-Fi",
                selected = wifiQ,
            ) { wifiQ = it; setWifiQuality(context, it) }

            QualityPicker(
                title = "Streaming over cellular",
                selected = cellQ,
            ) { cellQ = it; setCellularQuality(context, it) }

            QualityPicker(
                title = "Download quality",
                selected = dlQ,
            ) { dlQ = it; setDownloadQuality(context, it) }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoloShape.md)
                    .clickable {
                        com.music.spotui.di.SongPlayer.clearCaches(context)
                        android.widget.Toast.makeText(context, "Stream cache cleared", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    .background(SettingsCard)
                    .border(1.dp, SettingsHairline, SoloShape.md)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Clear Audio Stream Cache", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("Unlocks all cached streams and forces re-resolution", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear cache",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle("Playback")
            SettingsSwitchRow(
                title = "Allow video fallback",
                subtitle = "Use regular YouTube videos when a song result is not available",
                checked = videoFallback,
            ) {
                videoFallback = it
                setVideoFallbackEnabled(context, it)
            }
            SettingsSwitchRow(
                title = "Auto-play on startup",
                subtitle = "Resume playing the last track when the app opens",
                checked = autoPlay,
            ) {
                autoPlay = it
                setAutoPlayEnabled(context, it)
            }
            SettingsSwitchRow(
                title = "Normalize volume",
                subtitle = "Even out loud and quiet tracks using each song's loudness",
                checked = normalize,
            ) {
                normalize = it
                com.music.spotui.data.preferences.setNormalizeVolume(context, it)
            }
            SettingsClickRow(
                title = "Equalizer",
                subtitle = "Presets and a band-by-band sound curve",
                onClick = { navController.navigate(com.music.spotui.ui.navigation.Routes.Equalizer.route) },
            )

            Spacer(Modifier.height(12.dp))
            SectionTitle("Crossfade")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Crossfade", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    if (crossfadeMs <= 0f) "Off" else "${(crossfadeMs / 1000f).let { String.format("%.0f", it) }}s",
                    color = if (crossfadeMs <= 0f) TextSecondary else Accent,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                "Blend the end of a song into the start of the next",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = crossfadeMs,
                onValueChange = { crossfadeMs = it },
                onValueChangeFinished = { setCrossfadeMs(context, crossfadeMs.toInt()) },
                valueRange = 0f..CROSSFADE_MAX_MS.toFloat(),
                steps = (CROSSFADE_MAX_MS / 1000) - 1, // 1-second stops
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Surface4,
                ),
            )
            Spacer(Modifier.height(12.dp))
            SectionTitle("Playback diagnostics")
            Text(
                "If a song stops before it ends, open this straight afterwards. It records why " +
                    "playback stopped and how much of the stream arrived.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                com.music.spotui.ui.components.SoloPillButton(
                    text = "View playback log",
                    onClick = { showPlaybackLog = true },
                    primary = false,
                )
                Spacer(Modifier.width(8.dp))
                com.music.spotui.ui.components.SoloDialogDismiss(
                    text = "Clear",
                    onClick = {
                        PlaybackLog.clear()
                        android.widget.Toast
                            .makeText(context, "Playback log cleared", android.widget.Toast.LENGTH_SHORT)
                            .show()
                    },
                )
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle("Backup & Restore")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(SoloShape.md)
                        .clickable(enabled = !isBackingUp) {
                            if (backupDirUri.isNullOrBlank()) {
                                dirPickerLauncher.launch(null)
                            } else {
                                isBackingUp = true
                                scope.launch {
                                    val (success, message) = BackupHelper.performManualBackup(context)
                                    isBackingUp = false
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .background(SettingsCard)
                        .border(1.dp, SettingsHairline, SoloShape.md)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Back Up Now", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                isBackingUp -> "Creating backup in background…"
                                backupDirUri.isNullOrBlank() -> "Tap to choose folder & back up"
                                else -> "Folder: $folderName"
                            },
                            color = if (backupDirUri.isNullOrBlank()) com.music.spotui.ui.theme.Warning else TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                    if (isBackingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Back Up Now",
                            tint = Accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (!backupDirUri.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(SoloShape.md)
                            .background(SettingsCard)
                        .border(1.dp, SettingsHairline, SoloShape.md)
                            .clickable(enabled = !isBackingUp) { dirPickerLauncher.launch(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Change Backup Folder",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoloShape.md)
                    .clickable(enabled = !isRestoring) {
                        restoreFileLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                    .background(SettingsCard)
                        .border(1.dp, SettingsHairline, SoloShape.md)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Restore from File", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isRestoring) "Restoring backup in background…" else "Import playlists and settings from a SOLO backup file",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Accent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "Restore",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            SettingsSwitchRow(
                title = "Automatic backup",
                subtitle = if (backupDirUri.isNullOrBlank()) "Automatically backs up settings and playlists when app opens" else "Auto-backup saved to $folderName",
                checked = isAutoBackup,
            ) { enabled ->
                if (enabled && backupDirUri.isNullOrBlank()) {
                    dirPickerLauncher.launch(null)
                } else {
                    isAutoBackup = enabled
                    BackupPref.setAutoBackupEnabled(context, enabled)
                    if (enabled) {
                        scope.launch { BackupHelper.performAutoBackup(context) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle("Troubleshooting")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoloShape.md)
                    .clickable {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            com.music.spotui.di.SongPlayer.clearCaches(context)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "YouTube session & stream caches reset",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .background(SettingsCard)
                        .border(1.dp, SettingsHairline, SoloShape.md)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Reset YouTube & Bot Session", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Clears session tokens, visitor ID, PoToken generator, and resolved stream caches",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reset Session",
                    tint = Accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle("Account")
            // Spotify login is optional, the app runs login-free by default. Show
            // "Log in" when signed out (unlocks Home/Search/Library), or "Log out"
            // when a Spotify session exists.
            val loggedIn = com.music.spotui.data.api.SpotifySession.spDc(context).isNotBlank()
            if (loggedIn) {
                // Destructive row: Danger text and icon on a Surface1 card.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(SoloShape.md)
                        .background(SettingsCard)
                        .border(1.dp, SettingsHairline, SoloShape.md)
                        .clickable {
                            com.music.spotui.data.api.SpotifySession.setSpDc(context, "")
                            com.music.spotui.data.api.Api.HomeCache.clear()
                            navController.navigate(com.music.spotui.ui.navigation.Routes.YtSearch.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(SoloShape.sm)
                            .background(com.music.spotui.ui.theme.DangerSurface),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = com.music.spotui.ui.theme.Danger,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Log out of Spotify",
                        color = com.music.spotui.ui.theme.Danger,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoloShape.md)
                        .background(SettingsCard)
                        .border(1.dp, SettingsHairline, SoloShape.md)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(SoloShape.sm)
                            .background(Surface3),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = SettingsAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Free mode",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "You're using SOLO for free: search and play any song, no account needed.",
                            color = SettingsTextDim,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            SectionTitle("About")
            AboutCard()
            Spacer(Modifier.height(40.dp))
        }

        if (showDevicesSheet) {
            com.music.spotui.ui.components.DevicesSheet(
                context = context,
                onDismiss = { showDevicesSheet = false }
            )
        }

    }
}

/**
 * About card (compact): the SOLO Aperture S mark, name + version on one tight block, a
 * one-line tagline, and a single compact line that still carries the maintainer, the GPL-3.0
 * notice, the four project credits + fonts and the disclaimer. No source-code links.
 */
@Composable
private fun AboutCard() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(SoloShape.lg)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(com.music.spotui.ui.theme.Elevated, Surface2)
                )
            )
            .border(1.dp, com.music.spotui.ui.theme.HairlineAccent, SoloShape.lg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        com.music.spotui.ui.components.SoloMark(height = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SOLO",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "v${com.music.spotui.BuildConfig.VERSION_NAME} (${com.music.spotui.BuildConfig.VERSION_CODE})",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(1.dp))
            Text(
                "One voice. Pure sound.",
                color = Accent,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "By SATYAN SHARMA · GPL-3.0 · built on Neptune, Metrolist, SpotiFLAC, SimpMusic + open fonts · not affiliated with Spotify or YouTube.",
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 14.sp,
            )
        }
    }
}

// Shared surfaces so every settings group reads as one consistent card system.
private val SettingsAccent = com.music.spotui.ui.theme.Accent
private val SettingsCard = com.music.spotui.ui.theme.Surface1
private val SettingsHairline = com.music.spotui.ui.theme.Hairline
private val SettingsTextDim = TextSecondary

/**
 * Group heading. Small, uppercase and letter spaced so it reads as a label above a
 * card rather than competing with the row titles inside it.
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = SettingsAccent,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp)
    )
}

/**
 * A tappable settings row rendered as a premium card: a bordered surface with a title, a
 * subtitle, an optional leading icon, and an optional trailing slot (a chevron, a status
 * icon). Used so every actionable row in Settings shares one look instead of each being
 * styled inline with its own colours.
 */
@Composable
private fun SettingsClickRow(
    title: String,
    subtitle: String,
    subtitleColor: Color = SettingsTextDim,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SoloShape.md)
            .background(SettingsCard)
            .border(1.dp, SettingsHairline, SoloShape.md)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(SoloShape.sm)
                    .background(Surface3),
            ) { leadingIcon() }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = subtitleColor, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SoloShape.md)
            .background(SettingsCard)
            .border(1.dp, SettingsHairline, SoloShape.md)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = SettingsTextDim, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                // Dark thumb on the Volt track keeps the active state crisp.
                checkedThumbColor = OnAccent,
                checkedTrackColor = SettingsAccent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Surface3,
                uncheckedBorderColor = com.music.spotui.ui.theme.Hairline,
            ),
        )
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun QualityPicker(
    title: String,
    selected: StreamQuality,
    onSelect: (StreamQuality) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SoloShape.md)
            .background(SettingsCard)
            .border(1.dp, SettingsHairline, SoloShape.md)
            .padding(vertical = 12.dp),
    ) {
        Text(
            title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Spacer(Modifier.height(10.dp))
        StreamQuality.values().forEach { q ->
            val isSel = q == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(q) }
                    .heightIn(min = 52.dp)
                    .background(if (isSel) Surface4 else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        q.label,
                        color = if (isSel) SettingsAccent else TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(q.detail, color = SettingsTextDim, style = MaterialTheme.typography.bodySmall)
                }
                if (isSel) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = SettingsAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}


/**
 * Shows what playback recorded, newest first, with a copy button so the whole thing can be
 * pasted into a bug report. Read this right after a song cuts out: the last few lines say
 * whether the stream stopped short, the host refused a request, or the queue was advanced.
 */
@Composable
private fun PlaybackLogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lines = remember { PlaybackLog.lines() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        title = {
            Text(
                "Playback log",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            if (lines.isEmpty()) {
                Text(
                    "Nothing recorded yet. Play a song, and if it stops early come straight back here.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    lines.forEach { line ->
                        Text(
                            line,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            com.music.spotui.ui.components.SoloDialogConfirm(text = "Close", onClick = onDismiss)
        },
        dismissButton = {
            if (lines.isNotEmpty()) {
                TextButton(
                    onClick = {
                        val clipboard = context
                            .getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("SOLO playback log", PlaybackLog.asText()),
                        )
                        android.widget.Toast
                            .makeText(context, "Playback log copied", android.widget.Toast.LENGTH_SHORT)
                            .show()
                    },
                ) {
                    Text("Copy", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    )
}
