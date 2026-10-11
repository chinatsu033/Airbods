package com.chinatsu.airbods

import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import android.bluetooth.BluetoothDevice
import com.chinatsu.airbods.bluetooth.AdaptiveLevel
import com.chinatsu.airbods.bluetooth.BondedFinder
import com.chinatsu.airbods.bluetooth.NoiseMode
import com.chinatsu.airbods.data.BatteryHelp
import com.chinatsu.airbods.data.Permissions
import com.chinatsu.airbods.data.Prefs
import com.chinatsu.airbods.service.ConnectionService
import com.chinatsu.airbods.ui.HomeScreen
import com.chinatsu.airbods.ui.LastLocationScreen
import com.chinatsu.airbods.ui.SettingsScreen
import com.chinatsu.airbods.ui.theme.AirbodsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    private var hasBluetooth by mutableStateOf(false)
    private var hasNotifications by mutableStateOf(false)
    private var hasLocation by mutableStateOf(false)
    private var screen by mutableStateOf(Screen.Home)
    private var batteryIgnored by mutableStateOf(false)
    private var bondedHint by mutableStateOf("")
    private var showNotifRationale by mutableStateOf(false)
    private var locEpoch by mutableIntStateOf(0)

    private enum class Screen { Home, Settings, LastLocation }

    private val connectPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshPerms()
        refreshDevices()
        when {
            hasBluetooth && hasNotifications -> doConnect(preferFgs = true)
            hasBluetooth -> {
                Toast.makeText(this, getString(R.string.toast_fg_connect), Toast.LENGTH_LONG).show()
                doConnect(preferFgs = false)
            }
            else -> Toast.makeText(this, getString(R.string.toast_bt_required), Toast.LENGTH_LONG).show()
        }
    }

    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshPerms() }

    private val aclReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            refreshDevices()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        enableEdgeToEdge()
        refreshPerms()
        batteryIgnored = BatteryHelp.isIgnoring(this)
        refreshDevices()

        if (!hasBluetooth || !hasNotifications) {
            if (!hasNotifications && Build.VERSION.SDK_INT >= 33) showNotifRationale = true
            else requestConnectPerms()
        }
        if (intent?.getBooleanExtra(EXTRA_PROMPT_NOTIF, false) == true && !hasNotifications) {
            showNotifRationale = true
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_LAST_LOCATION, false) == true ||
            intent?.data?.host == "last-location"
        ) {
            screen = Screen.LastLocation
        }

        try {
            ContextCompat.registerReceiver(
                this,
                aclReceiver,
                IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } catch (t: Throwable) {
            Log.w(TAG, "ACL", t)
        }

        val app = application as AirbodsApp
        setContent {
            AirbodsTheme {
                if (showNotifRationale) {
                    AlertDialog(
                        onDismissRequest = {
                            showNotifRationale = false
                            requestConnectPerms()
                        },
                        title = { Text(getString(R.string.rationale_notif_title)) },
                        text = { Text(getString(R.string.rationale_notif_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showNotifRationale = false
                                requestConnectPerms()
                            }) { Text(getString(R.string.continue_btn)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showNotifRationale = false
                                requestConnectPerms()
                            }) { Text(getString(R.string.later)) }
                        },
                    )
                }

                val state by app.session.state.collectAsStateWithLifecycle()
                when (screen) {
                    Screen.LastLocation -> LastLocationScreen(
                        prefs = prefs,
                        hasLocation = hasLocation,
                        hasNotifications = hasNotifications,
                        refreshKey = locEpoch,
                        onBack = { screen = Screen.Home },
                        onOpenMap = { lat, lng -> openMap(lat, lng) },
                        onClear = { prefs.clearLastLocation(); locEpoch++ },
                        onRequestLocation = { locationPermLauncher.launch(Permissions.locationPermissions()) },
                        onRequestNotifications = { showNotifRationale = true },
                        onRefresh = { refreshPerms(); locEpoch++ },
                    )
                    Screen.Settings -> SettingsScreen(
                        hasBluetooth = hasBluetooth,
                        hasNotifications = hasNotifications,
                        hasLocation = hasLocation,
                        batteryIgnored = batteryIgnored,
                        onBack = {
                            batteryIgnored = BatteryHelp.isIgnoring(this)
                            screen = Screen.Home
                        },
                        onRequestConnectPerms = { requestConnectPerms() },
                        onRequestLocation = { locationPermLauncher.launch(Permissions.locationPermissions()) },
                        onOpenAppSettings = { Permissions.openAppSettings(this) },
                        onRequestBattery = {
                            try {
                                startActivity(BatteryHelp.requestIgnoreIntent(this))
                            } catch (_: Throwable) {
                                startActivity(BatteryHelp.appDetailsIntent(this))
                            }
                        },
                        onOpenSamsung = {
                            BatteryHelp.samsungAutostartIntents(this).firstOrNull()?.let {
                                try { startActivity(it) } catch (_: Throwable) {}
                            }
                        },
                        onOpenLastLocation = { screen = Screen.LastLocation },
                    )
                    Screen.Home -> HomeScreen(
                        state = state,
                        prefs = prefs,
                        hasBluetooth = hasBluetooth,
                        hasNotifications = hasNotifications,
                        bondedHint = bondedHint,
                        onConnect = { onConnectClicked() },
                        onDisconnect = {
                            lifecycleScope.launch { app.session.disconnect() }
                            if (hasNotifications) {
                                ConnectionService.start(this, ConnectionService.ACTION_DISCONNECT)
                            }
                        },
                        onMode = { mode ->
                            lifecycleScope.launch {
                                if (!app.session.state.value.connected) doConnect(preferFgs = hasNotifications)
                                app.session.setMode(
                                    mode,
                                    if (mode == NoiseMode.ADAPTIVE) AdaptiveLevel.BALANCED.level else null,
                                )
                            }
                            if (hasNotifications) {
                                ConnectionService.start(
                                    this,
                                    ConnectionService.ACTION_SET_MODE,
                                    mode = mode,
                                    adaptiveLevel = if (mode == NoiseMode.ADAPTIVE) AdaptiveLevel.BALANCED.level else null,
                                )
                            }
                        },
                        onAdaptiveLevel = { level ->
                            lifecycleScope.launch { app.session.setAdaptiveLevel(level) }
                            if (hasNotifications) {
                                ConnectionService.start(
                                    this,
                                    ConnectionService.ACTION_SET_ADAPTIVE,
                                    adaptiveLevel = level,
                                )
                            }
                        },
                        onCa = { enabled ->
                            lifecycleScope.launch { app.session.setConversationalAwareness(enabled) }
                            if (hasNotifications) {
                                ConnectionService.start(
                                    this,
                                    ConnectionService.ACTION_SET_CA,
                                    caEnabled = enabled,
                                )
                            }
                        },
                        onOpenSettings = {
                            batteryIgnored = BatteryHelp.isIgnoring(this)
                            screen = Screen.Settings
                        },
                        onOpenLastLocation = {
                            refreshPerms()
                            screen = Screen.LastLocation
                        },
                        onRequestPermissions = {
                            if (!hasNotifications) showNotifRationale = true
                            else requestConnectPerms()
                        },
                        onOpenAppSettings = { Permissions.openAppSettings(this) },
                        onRefreshDevices = { refreshDevices() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_PROMPT_NOTIF, false) && !Permissions.hasNotifications(this)) {
            showNotifRationale = true
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_LAST_LOCATION, false) || intent.data?.host == "last-location") {
            screen = Screen.LastLocation
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPerms()
        refreshDevices()
        batteryIgnored = BatteryHelp.isIgnoring(this)
    }

    override fun onDestroy() {
        try { unregisterReceiver(aclReceiver) } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun refreshPerms() {
        hasBluetooth = Permissions.hasBluetooth(this)
        hasNotifications = Permissions.hasNotifications(this)
        hasLocation = Permissions.hasLocation(this)
    }

    private fun refreshDevices() {
        bondedHint = if (!hasBluetooth) getString(R.string.devices_need_bt)
        else try { BondedFinder.describe(this) } catch (_: Throwable) { "—" }
    }

    private fun requestConnectPerms() {
        val needed = Permissions.connectPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            refreshPerms()
            return
        }
        try {
            connectPermLauncher.launch(needed.toTypedArray())
        } catch (t: Throwable) {
            Log.w(TAG, "perm", t)
        }
    }

    private fun onConnectClicked() {
        refreshPerms()
        if (!hasBluetooth) {
            requestConnectPerms()
            return
        }
        doConnect(preferFgs = hasNotifications)
    }

    private fun doConnect(preferFgs: Boolean) {
        refreshDevices()
        val device = BondedFinder.preferred(this)
        if (device == null) {
            Toast.makeText(this, getString(R.string.status_no_device), Toast.LENGTH_LONG).show()
            return
        }
        val app = application as AirbodsApp
        if (preferFgs && hasNotifications) {
            ConnectionService.start(this, ConnectionService.ACTION_CONNECT)
        } else {
            Toast.makeText(this, getString(R.string.toast_fg_connect), Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val ok = app.session.connect(device)
                if (!ok) {
                    Toast.makeText(
                        this@MainActivity,
                        app.session.state.value.lastError ?: getString(R.string.status_error),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun openMap(lat: Double, lng: Double) {
        try {
            val gmm = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(AirPods)"))
            if (gmm.resolveActivity(packageManager) != null) startActivity(gmm)
            else startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=17/$lat/$lng"),
                ),
            )
        } catch (t: Throwable) {
            Toast.makeText(this, t.message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_PROMPT_NOTIF = "prompt_notif"
        const val EXTRA_OPEN_LAST_LOCATION = "open_last_location"
    }
}
