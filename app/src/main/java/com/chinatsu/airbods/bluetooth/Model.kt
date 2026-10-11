package com.chinatsu.airbods.bluetooth

/**
 * AirPods model heuristics. Bare "AirPods Pro" is NOT Pro 1 —
 * modern Pro 2/3 often advertise that exact Bluetooth name.
 */
enum class AirPodsModel(
    val displayName: String,
    val productIds: Set<Int>,
    val hasAnc: Boolean,
    val hasAdaptive: Boolean,
    val hasAdaptiveStrength: Boolean,
    val hasConversationalAwareness: Boolean,
) {
    PRO_1("AirPods Pro", setOf(0x200E), true, false, false, false),
    PRO_2("AirPods Pro 2", setOf(0x2014, 0x2024), true, true, true, true),
    PRO_3("AirPods Pro 3", setOf(0x2027, 0x2031, 0x2032), true, true, true, true),
    AIRPODS_4_ANC("AirPods 4 ANC", setOf(0x201B), true, true, true, true),
    MAX_1("AirPods Max", setOf(0x200A, 0x201F), true, false, false, false),
    MAX_2("AirPods Max 2", setOf(0x202D), true, true, true, true),
    AIRPODS_3("AirPods 3", setOf(0x2013), false, false, false, false),
    UNKNOWN("AirPods", emptySet(), true, true, true, true);

    fun showAdaptive(): Boolean = when (this) {
        AIRPODS_3, MAX_1, PRO_1 -> false
        else -> true
    }

    fun showCa(): Boolean = hasConversationalAwareness || this == UNKNOWN ||
        this == PRO_2 || this == PRO_3 || this == AIRPODS_4_ANC || this == MAX_2

    companion object {
        fun resolve(productId: Int?, modelNumber: String?, btName: String?): AirPodsModel {
            if (productId != null) {
                entries.firstOrNull { productId in it.productIds }?.let { return it }
            }
            val m = modelNumber?.uppercase().orEmpty()
            when {
                m.startsWith("A3063") || m.startsWith("A3064") || m.startsWith("A3065") ||
                    m.startsWith("A3122") || m.startsWith("A3184") -> return PRO_3
                m.startsWith("A2698") || m.startsWith("A2699") || m.startsWith("A2700") ||
                    m.startsWith("A2931") || m.startsWith("A3047") || m.startsWith("A3048") ||
                    m.startsWith("A2968") -> return PRO_2
                m.startsWith("A2083") || m.startsWith("A2084") || m.startsWith("A2190") -> return PRO_1
                m.startsWith("A3055") || m.startsWith("A3056") || m.startsWith("A3057") ||
                    m.startsWith("A3059") -> return AIRPODS_4_ANC
                m.startsWith("A2096") -> return MAX_1
                m.startsWith("A3454") -> return MAX_2
                m.startsWith("A2564") || m.startsWith("A2565") || m.startsWith("A2566") -> return AIRPODS_3
            }
            return fromName(btName) ?: UNKNOWN
        }

        fun fromName(name: String?): AirPodsModel? {
            val n = name?.lowercase().orEmpty()
            if (n.isBlank()) return null
            return when {
                n.contains("pro 3") || n.contains("pro3") -> PRO_3
                n.contains("pro 2") || n.contains("pro2") -> PRO_2
                n.contains("pro 1") || n.contains("pro1") -> PRO_1
                n.contains("max 2") || n.contains("max2") -> MAX_2
                n.contains("max") -> MAX_1
                n.contains("airpods 4") || n.contains("airpod 4") -> AIRPODS_4_ANC
                n.contains("airpods 3") || n.contains("airpod 3") -> AIRPODS_3
                // Bare "AirPods Pro" → UNKNOWN (Adaptive on), never Pro 1
                n.contains("airpods pro") || (n.contains("pro") && n.contains("airpod")) -> UNKNOWN
                n.contains("airpods") || n.contains("airpod") -> UNKNOWN
                else -> null
            }
        }

        fun looksLikeAirPods(name: String?): Boolean = fromName(name) != null
    }
}
