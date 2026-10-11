package com.chinatsu.airbods.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LastLocation {
    private const val TAG = "LastLocation"

    @SuppressLint("MissingPermission")
    suspend fun capture(context: Context, deviceName: String?) = withContext(Dispatchers.IO) {
        if (!Permissions.hasLocation(context)) {
            Log.i(TAG, "no location permission")
            return@withContext
        }
        try {
            val lm = context.getSystemService(LocationManager::class.java) ?: return@withContext
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
            val loc = providers.firstNotNullOfOrNull { p ->
                try {
                    if (lm.isProviderEnabled(p)) lm.getLastKnownLocation(p) else null
                } catch (_: SecurityException) {
                    null
                } catch (_: Throwable) {
                    null
                }
            } ?: return@withContext

            val prefs = Prefs(context)
            prefs.lastLat = loc.latitude
            prefs.lastLng = loc.longitude
            prefs.lastLocationTimeMs = System.currentTimeMillis()
            prefs.lastLocationDeviceName = deviceName
            prefs.lastLocationAddress = reverseGeocode(context, loc.latitude, loc.longitude)
            Log.i(TAG, "saved ${loc.latitude},${loc.longitude}")
        } catch (t: Throwable) {
            Log.w(TAG, "capture failed", t)
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(context: Context, lat: Double, lng: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geo = Geocoder(context, Locale.getDefault())
            val list = if (Build.VERSION.SDK_INT >= 33) {
                // Sync path for simplicity on worker thread
                geo.getFromLocation(lat, lng, 1)
            } else {
                geo.getFromLocation(lat, lng, 1)
            }
            list?.firstOrNull()?.getAddressLine(0)
        } catch (_: Throwable) {
            null
        }
    }
}
