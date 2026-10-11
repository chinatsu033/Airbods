package com.chinatsu.airbods.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chinatsu.airbods.R
import com.chinatsu.airbods.bluetooth.AirPodsSession
import com.chinatsu.airbods.bluetooth.NoiseMode
import com.chinatsu.airbods.data.Prefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AirPodsSession.State,
    prefs: Prefs,
    hasBluetooth: Boolean,
    hasNotifications: Boolean,
    bondedHint: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onMode: (NoiseMode) -> Unit,
    onAdaptiveLevel: (Int) -> Unit,
    onCa: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLastLocation: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRefreshDevices: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                        Text(
                            state.model.displayName.takeIf { state.connected || state.deviceName != null }
                                ?: stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (state.connected) Icons.Outlined.Bluetooth else Icons.Outlined.BluetoothDisabled,
                        contentDescription = null,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.deviceName ?: stringResource(R.string.status_no_device_short),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            when {
                                !hasBluetooth -> stringResource(R.string.status_need_bt)
                                state.connecting -> stringResource(R.string.status_connecting)
                                state.connected && hasNotifications -> stringResource(R.string.status_connected)
                                state.connected -> stringResource(R.string.status_connected_fg)
                                state.lastError != null -> stringResource(R.string.status_error)
                                else -> stringResource(R.string.status_disconnected)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        state.battery?.let { b ->
                            val parts = buildList {
                                b.left?.let { add(stringResource(R.string.battery_l, it)) }
                                b.right?.let { add(stringResource(R.string.battery_r, it)) }
                                b.case?.let { add(stringResource(R.string.battery_c, it)) }
                            }
                            if (parts.isNotEmpty()) {
                                Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (!hasNotifications) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.banner_notif_title), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.banner_notif_body), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onRequestPermissions, shape = MaterialTheme.shapes.medium) {
                                Text(stringResource(R.string.grant_notifications))
                            }
                            OutlinedButton(onClick = onOpenAppSettings, shape = MaterialTheme.shapes.medium) {
                                Text(stringResource(R.string.app_settings))
                            }
                        }
                    }
                }
            }

            if (!hasBluetooth) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Text(stringResource(R.string.grant_bluetooth)) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onConnect,
                    enabled = hasBluetooth && !state.connecting,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(if (state.connected) stringResource(R.string.reconnect) else stringResource(R.string.connect))
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    enabled = state.connected || state.connecting,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Text(stringResource(R.string.disconnect)) }
            }

            TextButton(onClick = onRefreshDevices) {
                Text(stringResource(R.string.devices_found, bondedHint), style = MaterialTheme.typography.bodySmall)
            }

            ModeSwitcher(
                model = state.model,
                current = state.mode,
                adaptiveLevel = state.adaptiveLevel,
                enabled = state.connected || hasBluetooth,
                onMode = onMode,
                onAdaptiveLevel = onAdaptiveLevel,
            )

            if (state.model.showCa()) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.ca_title), fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(R.string.ca_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.conversationalAwareness == true,
                            onCheckedChange = onCa,
                            enabled = state.connected,
                        )
                    }
                }
            }

            Card(
                onClick = onOpenLastLocation,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Place, contentDescription = null)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.last_location), fontWeight = FontWeight.SemiBold)
                        Text(
                            if (prefs.hasLastLocation()) {
                                prefs.lastLocationAddress
                                    ?: stringResource(R.string.coords, prefs.lastLat, prefs.lastLng)
                            } else {
                                stringResource(R.string.last_location_card_empty)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.hint_qs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
