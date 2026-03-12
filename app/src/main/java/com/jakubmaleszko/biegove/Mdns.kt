package com.jakubmaleszko.biegove

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

data class Device(
    val name: String,
    val address: String
)

class MdnsHelper(private val context: Context) {
    private var jmdns: JmDNS? = null
    private var lock: WifiManager.MulticastLock? = null

    // Use a thread-safe way to stop discovery
    suspend fun startDiscovery(onDeviceFound: (Device) -> Unit) = withContext(Dispatchers.IO) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return@withContext

        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        lock = wifi.createMulticastLock("mdnsLock").apply {
            setReferenceCounted(true)
            acquire()
        }

        val ip = wifi.connectionInfo.ipAddress
        val addr = InetAddress.getByAddress(
            byteArrayOf((ip and 0xff).toByte(), (ip shr 8 and 0xff).toByte(), (ip shr 16 and 0xff).toByte(), (ip shr 24 and 0xff).toByte())
        )

        jmdns = JmDNS.create(addr)
        jmdns?.addServiceListener("_biegove._tcp.local.", object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent) { jmdns?.requestServiceInfo(event.type, event.name, 1) }
            override fun serviceRemoved(event: ServiceEvent) {}
            override fun serviceResolved(event: ServiceEvent) {
                val host = event.info.hostAddresses.firstOrNull { it.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) } ?: return
                // IMPORTANT: Use a callback that handles the UI thread if needed
                onDeviceFound(Device(event.name, "$host:${event.info.port}"))
            }
        })
    }

    suspend fun stopDiscovery() = withContext(Dispatchers.IO) {
        try {
            jmdns?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            jmdns = null
            lock?.release()
            lock = null
        }
    }
}