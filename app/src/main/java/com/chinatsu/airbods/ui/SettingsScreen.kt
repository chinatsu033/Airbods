package com.chinatsu.airbods.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chinatsu.airbods.R
import com.chinatsu.airbods.data.BatteryHelp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    hasBluetooth: Boolean,
    hasNotifications: Boolean,
    hasLocation: Boolean,
    batteryIgnored: Boolean,
    onBack: () -> Unit,
    onRequestConnectPerms: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRequestBattery: () -> Unit,
    onOpenSamsung: () -> Unit,
    onOpenLastLocation: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
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
            Section(stringResource(R.string.settings_permissions)) {
                Text((if (hasBluetooth) "✓ " else "○ ") + stringResource(R.string.perm_bt))
                Text((if (hasNotifications) "✓ " else "○ ") + stringResource(R.string.perm_notif))
                Text((if (hasLocation) "✓ " else "○ ") + stringResource(R.string.perm_loc))
                Button(onClick = onRequestConnectPerms, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.grant_connect_perms))
                }
                OutlinedButton(onClick = onRequestLocation, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.grant_location))
                }
                OutlinedButton(onClick = onOpenAppSettings, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.app_settings))
                }
            }

            Section(stringResource(R.string.last_location)) {
                Button(onClick = onOpenLastLocation, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.open_last_location))
                }
            }

            Section(stringResource(R.string.settings_battery)) {
                Text(
                    if (batteryIgnored) stringResource(R.string.battery_ok)
                    else stringResource(R.string.battery_restricted),
                )
                Text(stringResource(R.string.battery_help), style = MaterialTheme.typography.bodySmall)
                Button(onClick = onRequestBattery, shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.battery_request))
                }
                if (BatteryHelp.isSamsung()) {
                    Text(stringResource(R.string.samsung_help), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onOpenSamsung, shape = MaterialTheme.shapes.medium) {
                        Text(stringResource(R.string.samsung_open))
                    }
                }
            }

            Section(stringResource(R.string.settings_about)) {
                Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = { content() })
    }
}
