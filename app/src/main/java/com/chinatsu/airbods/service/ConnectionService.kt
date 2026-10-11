package com.chinatsu.airbods.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.chinatsu.airbods.AirbodsApp
import com.chinatsu.airbods.MainActivity
import com.chinatsu.airbods.R
import com.chinatsu.airbods.bluetooth.AdaptiveLevel
import com.chinatsu.airbods.bluetooth.BondedFinder
import com.chinatsu.airbods.bluetooth.NoiseMode
import com.chinatsu.airbods.data.LastLocation
import com.chinatsu.airbods.data.Permissions
import com.chinatsu.airbods.ui.ModePanelActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConnectionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val aclReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_ACL_DISCONNECTED) return
            val name = app().session.state.value.deviceName
            scope.launch(Dispatchers.IO) {
                LastLocation.capture(this@ConnectionService, name)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startFg()
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ContextCompat.registerReceiver(
                this,
                aclReceiver,
                IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } catch (_: Throwable) {
        }

        when (intent?.action) {
            ACTION_CONNECT -> scope.launch { connect() }
            ACTION_DISCONNECT -> scope.launch {
                val name = app().session.state.value.deviceName
                if (app().session.state.value.deviceAddress != null) {
                    LastLocation.capture(this@ConnectionService, name)
                }
                app().session.disconnect()
                stopSelf()
            }
            ACTION_SET_MODE -> {
                val wire = intent.getByteExtra(EXTRA_MODE, NoiseMode.ADAPTIVE.wireValue)
                val mode = NoiseMode.fromWire(wire) ?: NoiseMode.ADAPTIVE
                val level = if (intent.hasExtra(EXTRA_ADAPTIVE)) {
                    intent.getIntExtra(EXTRA_ADAPTIVE, AdaptiveLevel.BALANCED.level)
                } else null
                scope.launch {
                    ensureConnected()
                    app().session.setMode(mode, level)
                    refreshNotification()
                }
            }
            ACTION_SET_ADAPTIVE -> {
                val level = intent.getIntExtra(EXTRA_ADAPTIVE, AdaptiveLevel.BALANCED.level)
                scope.launch {
                    ensureConnected()
                    app().session.setAdaptiveLevel(level)
                    refreshNotification()
                }
            }
            ACTION_SET_CA -> {
                val en = intent.getBooleanExtra(EXTRA_CA, true)
                scope.launch {
                    ensureConnected()
                    app().session.setConversationalAwareness(en)
                }
            }
            else -> scope.launch { connect() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try { unregisterReceiver(aclReceiver) } catch (_: Throwable) {}
        scope.cancel()
        super.onDestroy()
    }

    private fun app() = application as AirbodsApp

    private suspend fun ensureConnected() {
        if (!app().session.state.value.connected) connect()
    }

    private suspend fun connect() {
        val device = BondedFinder.preferred(this)
        if (device == null) {
            refreshNotification(getString(R.string.status_no_device))
            return
        }
        app().session.connect(device)
        refreshNotification()
    }

    private fun startFg() {
        ensureChannel()
        val n = buildNotification(getString(R.string.status_connecting))
        val type = if (Build.VERSION.SDK_INT >= 34) {
            var t = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            if (Permissions.hasLocation(this)) {
                t = t or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            t
        } else 0
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceCompat.startForeground(this, NOTIF_ID, n, type)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "FGS type fallback", se)
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceCompat.startForeground(
                    this, NOTIF_ID, n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(NOTIF_ID, n)
            }
        }
    }

    private fun refreshNotification(text: String? = null) {
        val st = app().session.state.value
        val body = text ?: when {
            st.connecting -> getString(R.string.status_connecting)
            st.connected -> getString(R.string.status_connected) +
                (st.mode?.let { " · ${modeLabel(it)}" } ?: "")
            else -> getString(R.string.status_disconnected)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(body))
    }

    private fun modeLabel(mode: NoiseMode): String = when (mode) {
        NoiseMode.ANC -> getString(R.string.mode_anc)
        NoiseMode.TRANSPARENCY -> getString(R.string.mode_transparency)
        NoiseMode.ADAPTIVE -> getString(R.string.mode_adaptive)
        NoiseMode.OFF -> getString(R.string.mode_off)
    }

    private fun buildNotification(content: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, ModePanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val main = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, getString(R.string.action_open), main)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ch = NotificationChannel(
            CHANNEL,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.notification_channel_desc) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    companion object {
        private const val TAG = "ConnectionService"
        private const val CHANNEL = "airbods_connection"
        private const val NOTIF_ID = 42

        const val ACTION_CONNECT = "com.chinatsu.airbods.CONNECT"
        const val ACTION_DISCONNECT = "com.chinatsu.airbods.DISCONNECT"
        const val ACTION_SET_MODE = "com.chinatsu.airbods.SET_MODE"
        const val ACTION_SET_ADAPTIVE = "com.chinatsu.airbods.SET_ADAPTIVE"
        const val ACTION_SET_CA = "com.chinatsu.airbods.SET_CA"
        const val EXTRA_MODE = "mode"
        const val EXTRA_ADAPTIVE = "adaptive"
        const val EXTRA_CA = "ca"

        fun start(
            context: Context,
            action: String = ACTION_CONNECT,
            mode: NoiseMode? = null,
            adaptiveLevel: Int? = null,
            caEnabled: Boolean? = null,
        ) {
            if (!Permissions.hasNotifications(context) && Build.VERSION.SDK_INT >= 33) {
                Log.w(TAG, "skip FGS — no notification permission")
                return
            }
            try {
                val i = Intent(context, ConnectionService::class.java).setAction(action)
                if (mode != null) i.putExtra(EXTRA_MODE, mode.wireValue)
                if (adaptiveLevel != null) i.putExtra(EXTRA_ADAPTIVE, adaptiveLevel)
                if (caEnabled != null) i.putExtra(EXTRA_CA, caEnabled)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "start failed", t)
            }
        }
    }
}
