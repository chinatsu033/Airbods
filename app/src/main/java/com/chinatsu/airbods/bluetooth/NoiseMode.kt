package com.chinatsu.airbods.bluetooth

/** AACP listening mode (control 0x0D). Values from public AACP captures. */
enum class NoiseMode(val wireValue: Byte) {
    OFF(0x01),
    ANC(0x02),
    TRANSPARENCY(0x03),
    ADAPTIVE(0x04);

    companion object {
        fun fromWire(value: Byte): NoiseMode? = entries.firstOrNull { it.wireValue == value }
    }
}

enum class AdaptiveLevel(val level: Int) {
    MORE_TRANSPARENCY(0),
    BALANCED(50),
    MORE_ANC(100);

    companion object {
        fun nearest(level: Int): AdaptiveLevel =
            entries.minBy { kotlin.math.abs(it.level - level) }
    }
}
