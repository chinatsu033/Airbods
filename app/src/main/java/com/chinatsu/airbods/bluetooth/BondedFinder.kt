package com.chinatsu.airbods.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BondedFinder {
    @SuppressLint("MissingPermission")
    fun list(context: Context): List<BluetoothDevice> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
            if (!ok) return emptyList()
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
            ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        val bonded = try {
            adapter.bondedDevices?.toList().orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
        val byName = bonded.filter { d ->
            AirPodsModel.looksLikeAirPods(safeName(d)) ||
                AirPodsModel.looksLikeAirPods(safeAlias(d))
        }.sortedWith(compareByDescending<BluetoothDevice> { isConnected(it) }.thenBy { safeName(it) })
        if (byName.isNotEmpty()) return byName
        val connectedAudio = bonded.filter { isConnected(it) && isAudio(it) }
        return if (connectedAudio.size == 1) connectedAudio else emptyList()
    }

    fun preferred(context: Context): BluetoothDevice? {
        val list = list(context)
        return list.firstOrNull { isConnected(it) } ?: list.firstOrNull()
    }

    fun describe(context: Context): String =
        list(context).joinToString { "${safeName(it) ?: "?"} (${if (isConnected(it)) "on" else "bonded"})" }
            .ifBlank { "—" }

    @SuppressLint("MissingPermission")
    private fun safeName(d: BluetoothDevice): String? = try { d.name } catch (_: SecurityException) { null }

    @SuppressLint("MissingPermission")
    private fun safeAlias(d: BluetoothDevice): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try { d.alias } catch (_: Throwable) { null }
        } else null

    private fun isConnected(d: BluetoothDevice): Boolean = try {
        BluetoothDevice::class.java.getMethod("isConnected").invoke(d) as? Boolean == true
    } catch (_: Throwable) { false }

    private fun isAudio(d: BluetoothDevice): Boolean {
        val bc = d.bluetoothClass ?: return false
        return bc.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
    }
}
