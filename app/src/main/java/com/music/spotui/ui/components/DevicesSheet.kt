package com.music.spotui.ui.components

import androidx.compose.material.icons.rounded.Devices
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.R
import com.music.spotui.ui.utils.AudioDeviceHelper
import com.music.spotui.ui.utils.AudioDeviceItem
import com.music.spotui.ui.utils.AudioDeviceType
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesSheet(
    context: Context,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var devices by remember { mutableStateOf<List<AudioDeviceItem>>(emptyList()) }
    var hasBtPermission by remember { mutableStateOf(AudioDeviceHelper.hasBluetoothPermission(context)) }

    fun refreshDevices() {
        devices = AudioDeviceHelper.getAvailableAudioDevices(context)
    }

    LaunchedEffect(Unit) {
        refreshDevices()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBtPermission = isGranted
        refreshDevices()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = 560.dp,
        containerColor = com.music.spotui.ui.theme.Surface2,
        contentColor = TextPrimary,
        shape = com.music.spotui.ui.theme.SoloShape.sheetTop,
        dragHandle = { com.music.spotui.ui.components.SoloDragHandle() },
        scrimColor = com.music.spotui.ui.theme.Scrim,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // Header: Spotify-styled title + close icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Devices),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Connect to a device",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge)
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() }
                        .padding(12.dp)
                )
            }

            HorizontalDivider(color = com.music.spotui.ui.theme.Hairline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Bluetooth Permission Banner if needed (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBtPermission) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoloShape.sm)
                        .background(Surface3)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Grant permission to discover paired Bluetooth devices",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(SoloShape.pill)
                            .background(com.music.spotui.ui.theme.AccentBrush)
                            .clickable {
                                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Grant",
                            color = com.music.spotui.ui.theme.OnAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Current Active Device Title
            val activeDevice = devices.firstOrNull { it.isActive }
            if (activeDevice != null) {
                Text(
                    text = "CURRENT DEVICE",
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoloShape.md)
                        .background(com.music.spotui.ui.theme.Surface4)
                        .border(1.dp, com.music.spotui.ui.theme.HairlineAccent, SoloShape.md)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Devices),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeDevice.name,
                            color = Accent,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Listening on this device",
                            color = Accent.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active",
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Available Devices Section Title
            Text(
                text = "SELECT A DEVICE",
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Devices list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(weight = 1f, fill = false)
            ) {
                items(devices) { item ->
                    DeviceItemRow(
                        item = item,
                        onClick = {
                            val success = AudioDeviceHelper.switchAudioOutput(context, item)
                            if (!success) {
                                AudioDeviceHelper.openSystemAudioSwitcher(context)
                            }
                            refreshDevices()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = com.music.spotui.ui.theme.Hairline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Open System Audio Switcher / Bluetooth Settings Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoloShape.sm)
                    .clickable {
                        AudioDeviceHelper.openSystemAudioSwitcher(context)
                        onDismiss()
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Surface3)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "System Audio Switcher",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Connect or pair Bluetooth devices in Android Settings",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DeviceItemRow(
    item: AudioDeviceItem,
    onClick: () -> Unit
) {
    val textColor = if (item.isActive) Accent else TextPrimary
    val iconColor = if (item.isActive) Accent else TextSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(SoloShape.sm)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Icon according to device type
        when (item.type) {
            AudioDeviceType.SPEAKER -> {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            AudioDeviceType.WIRED_HEADPHONES -> {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            AudioDeviceType.BLUETOOTH, AudioDeviceType.OTHER -> {
                Icon(
                    painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Devices),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subText = when {
                item.isActive -> "Active audio route"
                item.isConnected -> "Connected"
                else -> "Paired Bluetooth Device"
            }
            Text(
                text = subText,
                color = if (item.isActive) Accent.copy(alpha = 0.8f) else TextTertiary,
                style = MaterialTheme.typography.bodySmall)
        }

        if (item.isActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Active",
                tint = Accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
