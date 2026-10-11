package com.chinatsu.airbods.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method

object L2capSocket {
    private const val TAG = "L2capSocket"
    @Volatile private var exempted = false

    @Synchronized
    private fun ensureExempt() {
        if (exempted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("Landroid/bluetooth/")
            } catch (t: Throwable) {
                Log.w(TAG, "HiddenApiBypass", t)
            }
        }
        exempted = true
    }

    fun open(device: BluetoothDevice, psm: Int = Aacp.L2CAP_PSM): BluetoothSocket {
        ensureExempt()
        var last: Throwable? = null
        for (name in listOf("createL2capSocket", "createInsecureL2capSocket")) {
            try {
                val m: Method = BluetoothDevice::class.java.getMethod(name, Int::class.javaPrimitiveType)
                return m.invoke(device, psm) as BluetoothSocket
            } catch (t: Throwable) {
                last = t
            }
        }
        try {
            @Suppress("UNCHECKED_CAST")
            return HiddenApiBypass.newInstance(
                BluetoothSocket::class.java,
                3, -1, true, true, device, psm, null,
            ) as BluetoothSocket
        } catch (t: Throwable) {
            last = t
        }
        throw IllegalStateException("Cannot open L2CAP PSM=0x${psm.toString(16)}: ${last?.message}", last)
    }
}
