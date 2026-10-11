package com.chinatsu.airbods.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class AirPodsSession(private val scope: CoroutineScope) {
    data class State(
        val connected: Boolean = false,
        val connecting: Boolean = false,
        val deviceName: String? = null,
        val deviceAddress: String? = null,
        val model: AirPodsModel = AirPodsModel.UNKNOWN,
        val mode: NoiseMode? = null,
        val adaptiveLevel: Int = AdaptiveLevel.BALANCED.level,
        val conversationalAwareness: Boolean? = null,
        val battery: Aacp.Battery? = null,
        val lastError: String? = null,
        val modelNumber: String? = null,
        val firmware: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val writeMutex = Mutex()
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var readerJob: Job? = null

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        disconnectInternal()
        val name = try { device.name } catch (_: SecurityException) { null }
        _state.value = _state.value.copy(
            connecting = true,
            lastError = null,
            deviceName = name,
            deviceAddress = device.address,
            model = AirPodsModel.resolve(null, null, name),
        )
        try {
            val sock = L2capSocket.open(device)
            sock.connect()
            socket = sock
            input = sock.inputStream
            output = sock.outputStream
            writeRaw(Aacp.HANDSHAKE)
            writeRaw(Aacp.FEATURE_ENABLE)
            writeRaw(Aacp.REQUEST_NOTIFICATIONS)
            writeRaw(Aacp.LISTENING_MODE_CONFIGS_ALL)
            _state.value = _state.value.copy(connected = true, connecting = false)
            startReader()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "connect", t)
            disconnectInternal()
            _state.value = _state.value.copy(
                connected = false,
                connecting = false,
                lastError = t.message ?: t.javaClass.simpleName,
            )
            false
        }
    }

    suspend fun setMode(mode: NoiseMode, adaptiveLevel: Int? = null): Boolean = withContext(Dispatchers.IO) {
        if (!_state.value.connected) {
            _state.value = _state.value.copy(lastError = "Not connected")
            return@withContext false
        }
        try {
            if (mode == NoiseMode.ADAPTIVE) {
                writeRaw(Aacp.FEATURE_ENABLE)
                writeRaw(Aacp.LISTENING_MODE_CONFIGS_ALL)
            }
            writeRaw(Aacp.setListeningMode(mode))
            if (mode == NoiseMode.ADAPTIVE) {
                val level = (adaptiveLevel ?: _state.value.adaptiveLevel).coerceIn(0, 100)
                writeRaw(Aacp.setAdaptiveNoiseLevel(level))
                _state.value = _state.value.copy(mode = mode, adaptiveLevel = level, lastError = null)
            } else {
                _state.value = _state.value.copy(mode = mode, lastError = null)
            }
            true
        } catch (t: Throwable) {
            _state.value = _state.value.copy(lastError = t.message ?: t.javaClass.simpleName)
            false
        }
    }

    suspend fun setAdaptiveLevel(level: Int): Boolean = withContext(Dispatchers.IO) {
        if (!_state.value.connected) return@withContext false
        try {
            val clamped = level.coerceIn(0, 100)
            if (_state.value.mode != NoiseMode.ADAPTIVE) {
                writeRaw(Aacp.FEATURE_ENABLE)
                writeRaw(Aacp.LISTENING_MODE_CONFIGS_ALL)
                writeRaw(Aacp.setListeningMode(NoiseMode.ADAPTIVE))
            }
            writeRaw(Aacp.setAdaptiveNoiseLevel(clamped))
            _state.value = _state.value.copy(mode = NoiseMode.ADAPTIVE, adaptiveLevel = clamped, lastError = null)
            true
        } catch (t: Throwable) {
            _state.value = _state.value.copy(lastError = t.message ?: t.javaClass.simpleName)
            false
        }
    }

    suspend fun setConversationalAwareness(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!_state.value.connected) return@withContext false
        try {
            writeRaw(Aacp.FEATURE_ENABLE)
            writeRaw(Aacp.setConversationalAwareness(enabled))
            _state.value = _state.value.copy(conversationalAwareness = enabled, lastError = null)
            true
        } catch (t: Throwable) {
            _state.value = _state.value.copy(lastError = t.message ?: t.javaClass.simpleName)
            false
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) { disconnectInternal() }

    private fun disconnectInternal() {
        readerJob?.cancel()
        readerJob = null
        try { input?.close() } catch (_: Throwable) {}
        try { output?.close() } catch (_: Throwable) {}
        try { socket?.close() } catch (_: Throwable) {}
        input = null
        output = null
        socket = null
        _state.value = _state.value.copy(connected = false, connecting = false)
    }

    private suspend fun writeRaw(bytes: ByteArray) = writeMutex.withLock {
        val out = output ?: error("no output")
        out.write(bytes)
        out.flush()
    }

    private fun startReader() {
        readerJob?.cancel()
        readerJob = scope.launch(Dispatchers.IO) {
            val buf = ByteArray(1024)
            val inp = input ?: return@launch
            while (isActive) {
                val n = try { inp.read(buf) } catch (_: Throwable) { -1 }
                if (n <= 0) break
                val packet = buf.copyOf(n)
                Aacp.parse(
                    packet,
                    onMode = { m -> _state.value = _state.value.copy(mode = m) },
                    onBattery = { b -> _state.value = _state.value.copy(battery = b) },
                    onCa = { en -> _state.value = _state.value.copy(conversationalAwareness = en) },
                    onAdaptive = { lv -> _state.value = _state.value.copy(adaptiveLevel = lv) },
                    onInfo = { info ->
                        val model = AirPodsModel.resolve(null, info.modelNumber, info.name ?: _state.value.deviceName)
                        _state.value = _state.value.copy(
                            model = model,
                            modelNumber = info.modelNumber,
                            firmware = info.firmware,
                            deviceName = info.name ?: _state.value.deviceName,
                        )
                    },
                )
            }
            _state.value = _state.value.copy(connected = false)
        }
    }

    companion object {
        private const val TAG = "AirPodsSession"
    }
}
