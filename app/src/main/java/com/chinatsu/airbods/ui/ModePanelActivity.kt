package com.chinatsu.airbods.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.chinatsu.airbods.AirbodsApp
import com.chinatsu.airbods.MainActivity
import com.chinatsu.airbods.R
import com.chinatsu.airbods.bluetooth.AdaptiveLevel
import com.chinatsu.airbods.bluetooth.BondedFinder
import com.chinatsu.airbods.bluetooth.NoiseMode
import com.chinatsu.airbods.data.Permissions
import com.chinatsu.airbods.service.ConnectionService
import com.chinatsu.airbods.ui.theme.AirbodsTheme
import kotlinx.coroutines.launch

/** Solid Material panel opened from the single Airbods QS tile / notification. */
class ModePanelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AirbodsApp
        val hasNotif = Permissions.hasNotifications(this)
        val hasBt = Permissions.hasBluetooth(this)
        if (hasNotif && hasBt) {
            ConnectionService.start(this)
        }
        setContent {
            AirbodsTheme {
                val state by app.session.state.collectAsStateWithLifecycle()
                Panel(
                    stateName = state.deviceName ?: state.model.displayName,
                    connected = state.connected,
                    model = state.model,
                    mode = state.mode,
                    adaptiveLevel = state.adaptiveLevel,
                    needsNotif = !hasNotif,
                    onDismiss = { finish() },
                    onConnect = {
                        lifecycleScope.launch {
                            if (hasNotif) ConnectionService.start(this@ModePanelActivity)
                            else {
                                BondedFinder.preferred(this@ModePanelActivity)?.let {
                                    app.session.connect(it)
                                }
                            }
                        }
                    },
                    onMode = { mode ->
                        lifecycleScope.launch {
                            if (!app.session.state.value.connected) {
                                if (hasNotif) ConnectionService.start(this@ModePanelActivity)
                                else BondedFinder.preferred(this@ModePanelActivity)?.let { app.session.connect(it) }
                            }
                            app.session.setMode(
                                mode,
                                if (mode == NoiseMode.ADAPTIVE) AdaptiveLevel.BALANCED.level else null,
                            )
                        }
                        if (hasNotif) {
                            ConnectionService.start(
                                this,
                                ConnectionService.ACTION_SET_MODE,
                                mode = mode,
                                adaptiveLevel = if (mode == NoiseMode.ADAPTIVE) AdaptiveLevel.BALANCED.level else null,
                            )
                        }
                    },
                    onAdaptive = { level ->
                        lifecycleScope.launch { app.session.setAdaptiveLevel(level) }
                        if (hasNotif) {
                            ConnectionService.start(
                                this,
                                ConnectionService.ACTION_SET_ADAPTIVE,
                                adaptiveLevel = level,
                            )
                        }
                    },
                    onOpenApp = {
                        startActivity(
                            android.content.Intent(this, MainActivity::class.java)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(MainActivity.EXTRA_PROMPT_NOTIF, !hasNotif),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun Panel(
    stateName: String,
    connected: Boolean,
    model: com.chinatsu.airbods.bluetooth.AirPodsModel,
    mode: NoiseMode?,
    adaptiveLevel: Int,
    needsNotif: Boolean,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onMode: (NoiseMode) -> Unit,
    onAdaptive: (Int) -> Unit,
    onOpenApp: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (connected) stringResource(R.string.status_connected) else stringResource(R.string.status_disconnected),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stateName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (needsNotif) {
                    Text(stringResource(R.string.qs_notif_hint), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onOpenApp) { Text(stringResource(R.string.grant_notifications)) }
                }
                if (!connected) {
                    TextButton(onClick = onConnect) { Text(stringResource(R.string.connect)) }
                }
                ModeSwitcher(
                    model = model,
                    current = mode,
                    adaptiveLevel = adaptiveLevel,
                    enabled = true,
                    onMode = onMode,
                    onAdaptiveLevel = onAdaptive,
                )
            }
        }
    }
}
