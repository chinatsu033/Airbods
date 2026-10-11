package com.chinatsu.airbods

import android.app.Application
import com.chinatsu.airbods.bluetooth.AirPodsSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AirbodsApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var session: AirPodsSession
        private set

    override fun onCreate() {
        super.onCreate()
        session = AirPodsSession(appScope)
    }
}
