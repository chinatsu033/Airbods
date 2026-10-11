package com.chinatsu.airbods.bluetooth

/**
 * Apple Accessory Communication Protocol helpers.
 * Reimplemented from publicly documented AACP packet layouts (MIT).
 * Transport: classic BR/EDR L2CAP PSM 0x1001.
 */
object Aacp {
    const val L2CAP_PSM: Int = 0x1001

    val HANDSHAKE = byteArrayOf(
        0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x02, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    )

    val FEATURE_ENABLE = byteArrayOf(
        0x04, 0x00, 0x04, 0x00, 0x4D, 0x00, 0xFF.toByte(),
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    )

    val REQUEST_NOTIFICATIONS = byteArrayOf(
        0x04, 0x00, 0x04, 0x00, 0x0F, 0x00,
        0xFF.toByte(), 0xFF.toByte(), 0xFE.toByte(), 0xFF.toByte(),
    )

    val LISTENING_MODE_CONFIGS_ALL = control(0x1A, 0x0F)

    fun setListeningMode(mode: NoiseMode) = control(0x0D, mode.wireValue.toInt() and 0xFF)
    fun setAdaptiveNoiseLevel(level: Int) = control(0x2E, level.coerceIn(0, 100))
    fun setConversationalAwareness(enabled: Boolean) = control(0x28, if (enabled) 0x01 else 0x02)

    fun control(id: Int, data1: Int, data2: Int = 0): ByteArray = byteArrayOf(
        0x04, 0x00, 0x04, 0x00, 0x09, 0x00,
        (id and 0xFF).toByte(),
        (data1 and 0xFF).toByte(),
        (data2 and 0xFF).toByte(),
        0x00, 0x00,
    )

    data class Battery(val left: Int? = null, val right: Int? = null, val case: Int? = null)
    data class DeviceInfo(
        val name: String? = null,
        val modelNumber: String? = null,
        val firmware: String? = null,
    )

    fun parse(
        packet: ByteArray,
        onMode: (NoiseMode) -> Unit,
        onBattery: (Battery) -> Unit,
        onCa: (Boolean) -> Unit = {},
        onAdaptive: (Int) -> Unit = {},
        onInfo: (DeviceInfo) -> Unit = {},
    ): Boolean {
        if (packet.size < 6) return false
        if (packet[0] != 0x04.toByte() || packet[2] != 0x04.toByte()) return false
        val opcode = (packet[4].toInt() and 0xFF) or ((packet[5].toInt() and 0xFF) shl 8)
        when (opcode) {
            0x0009 -> {
                if (packet.size < 8) return false
                when (packet[6].toInt() and 0xFF) {
                    0x0D -> { NoiseMode.fromWire(packet[7])?.let(onMode); return true }
                    0x28 -> {
                        when (packet[7].toInt() and 0xFF) {
                            0x01 -> onCa(true)
                            0x02 -> onCa(false)
                        }
                        return true
                    }
                    0x2E -> { onAdaptive(packet[7].toInt() and 0xFF); return true }
                }
            }
            0x0004 -> {
                if (packet.size < 7) return false
                val count = packet[6].toInt() and 0xFF
                var offset = 7
                var left: Int? = null
                var right: Int? = null
                var case: Int? = null
                repeat(count) {
                    if (offset + 4 >= packet.size) return@repeat
                    when (packet[offset].toInt() and 0xFF) {
                        0x04 -> left = (packet[offset + 2].toInt() and 0xFF).coerceIn(0, 100)
                        0x02 -> right = (packet[offset + 2].toInt() and 0xFF).coerceIn(0, 100)
                        0x08 -> case = (packet[offset + 2].toInt() and 0xFF).coerceIn(0, 100)
                    }
                    offset += 5
                }
                onBattery(Battery(left, right, case))
                return true
            }
            0x001D -> {
                val strings = nullTerminatedStrings(packet, 6)
                if (strings.isNotEmpty()) {
                    onInfo(
                        DeviceInfo(
                            name = strings.getOrNull(0),
                            modelNumber = strings.getOrNull(1),
                            firmware = strings.getOrNull(4),
                        ),
                    )
                    return true
                }
            }
        }
        return false
    }

    private fun nullTerminatedStrings(packet: ByteArray, start: Int): List<String> {
        val out = mutableListOf<String>()
        var i = start
        while (i < packet.size && (packet[i].toInt() and 0xFF) < 0x20) i++
        val chunk = StringBuilder()
        while (i < packet.size && out.size < 8) {
            val b = packet[i].toInt() and 0xFF
            when {
                b == 0 -> {
                    if (chunk.isNotEmpty()) { out += chunk.toString(); chunk.clear() }
                }
                b in 0x20..0x7E || b >= 0x80 -> chunk.append(b.toChar())
                chunk.isNotEmpty() -> { out += chunk.toString(); chunk.clear() }
            }
            i++
        }
        if (chunk.isNotEmpty()) out += chunk.toString()
        return out
    }
}
