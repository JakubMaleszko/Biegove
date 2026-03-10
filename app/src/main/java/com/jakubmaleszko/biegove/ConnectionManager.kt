package com.jakubmaleszko.biegove

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ConnectionManager {
    private val _connectedDevice = MutableStateFlow<Device?>(null)
    val connectedDevice: StateFlow<Device?> = _connectedDevice

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var pingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun connect(device: Device): Boolean {
        Log.d("CONNECTION", "Connecting to ${device.address}")
        return withContext(Dispatchers.IO) {
            try {
                val result = post("http://${device.address}/handshake", "{}")
                if (result != null) {
                    _connectedDevice.value = device
                    _isConnected.value = true
                    startPinging(device)
                    true
                } else {
                    _isConnected.value = false
                    false
                }
            } catch (_: Exception) {
                _isConnected.value = false
                false
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        _connectedDevice.value = null
        _isConnected.value = false
    }

    private fun startPinging(device: Device) {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(5000)
                try {
                    val result = post("http://${device.address}/ping", "{}")
                    if (result == null) {
                        _isConnected.value = false
                        _connectedDevice.value = null
                        break
                    }
                } catch (_: Exception) {
                    _isConnected.value = false
                    _connectedDevice.value = null
                    break
                }
            }
        }
    }

    fun syncData(entries: List<Pair<Int, Long>>) {
        val device = _connectedDevice.value ?: return
        scope.launch {
            try {
                val json = JSONObject()
                val arr = JSONArray()
                entries.forEach { (number, timestamp) ->
                    val obj = JSONObject()
                    obj.put("number", number)
                    obj.put("timestamp", timestamp)
                    arr.put(obj)
                }
                json.put("entries", arr)
                post("http://${device.address}/sync", json.toString())
            } catch (_: Exception) { }
        }
    }

    private fun post(url: String, body: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        conn.outputStream.use { it.write(body.toByteArray()) }
        return if (conn.responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else null
    }
}
