package com.chinatsu.airbods.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.chinatsu.airbods.AirbodsApp
import com.chinatsu.airbods.R
import com.chinatsu.airbods.ui.ModePanelActivity

/** Single Quick Settings tile — opens the Material mode panel. */
class AirbodsTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, ModePanelActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val launch = {
            try {
                val pi = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                if (Build.VERSION.SDK_INT >= 34) {
                    startActivityAndCollapse(pi)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            } catch (_: Throwable) {
                try { startActivity(intent) } catch (_: Throwable) {}
            }
        }
        try {
            unlockAndRun { launch() }
        } catch (_: Throwable) {
            launch()
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val connected = try {
            (application as? AirbodsApp)?.session?.state?.value?.connected == true
        } catch (_: Throwable) {
            false
        }
        tile.label = getString(R.string.app_name)
        tile.contentDescription = getString(R.string.tile_cd)
        tile.state = if (connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
