package com.chinatsu.airbods.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var lastLat: Double
        get() = Double.fromBits(sp.getLong(KEY_LAT, Double.NaN.toRawBits()))
        set(v) { sp.edit().putLong(KEY_LAT, v.toRawBits()).apply() }

    var lastLng: Double
        get() = Double.fromBits(sp.getLong(KEY_LNG, Double.NaN.toRawBits()))
        set(v) { sp.edit().putLong(KEY_LNG, v.toRawBits()).apply() }

    var lastLocationTimeMs: Long
        get() = sp.getLong(KEY_TIME, 0L)
        set(v) { sp.edit().putLong(KEY_TIME, v).apply() }

    var lastLocationAddress: String?
        get() = sp.getString(KEY_ADDR, null)
        set(v) { sp.edit().putString(KEY_ADDR, v).apply() }

    var lastLocationDeviceName: String?
        get() = sp.getString(KEY_NAME, null)
        set(v) { sp.edit().putString(KEY_NAME, v).apply() }

    fun hasLastLocation(): Boolean =
        lastLocationTimeMs > 0L && !lastLat.isNaN() && !lastLng.isNaN()

    fun clearLastLocation() {
        sp.edit()
            .remove(KEY_LAT).remove(KEY_LNG).remove(KEY_TIME).remove(KEY_ADDR).remove(KEY_NAME)
            .apply()
    }

    companion object {
        private const val NAME = "airbods_prefs"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_TIME = "time"
        private const val KEY_ADDR = "addr"
        private const val KEY_NAME = "dev"
    }
}
