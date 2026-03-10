package com.jakubmaleszko.biegove

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

data class Device(
    val name: String,
    val address: String
)

class MdnsHelper(private val context: Context) {

    fun startDiscovery(onDeviceFound: (Device) -> Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if(isConnected){
            Thread {

                val wifi = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                val lock = wifi.createMulticastLock("mdnsLock")
                lock.setReferenceCounted(true)
                lock.acquire()

                val ip = wifi.connectionInfo.ipAddress
                val addr = InetAddress.getByAddress(
                    byteArrayOf(
                        (ip and 0xff).toByte(),
                        (ip shr 8 and 0xff).toByte(),
                        (ip shr 16 and 0xff).toByte(),
                        (ip shr 24 and 0xff).toByte()
                    )
                )

                val jmdns = JmDNS.create(addr)

                jmdns.addServiceListener("_biegove._tcp.local.", object : ServiceListener {

                    override fun serviceAdded(event: ServiceEvent) {
                        jmdns.requestServiceInfo(event.type, event.name, 1)
                    }

                    override fun serviceRemoved(event: ServiceEvent) {}

                    override fun serviceResolved(event: ServiceEvent) {
                        val host = event.info.hostAddresses
                            .firstOrNull { it.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) }
                            ?: return
                        val port = event.info.port
                        val name = event.name
                        onDeviceFound(Device(name, "$host:$port"))
                    }
                })
            }.start()
        }
    }
}