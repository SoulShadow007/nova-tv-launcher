package com.novatv.launcher.system

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import java.io.File

/**
 * Real device metrics sampled on demand:
 * RAM usage, SoC temperature (battery sensor or thermal zone),
 * live download speed and VPN state. Works on most TV boxes.
 */
class SystemInfoMonitor(private val context: Context) {

    data class State(
        val ramUsedPct: Int,
        val ramAvail: Long,
        val tempC: Int?,
        val downBps: Float?,
        val vpnActive: Boolean
    )

    private var lastRx = -1L
    private var lastTx = -1L
    private var lastTs = -1L

    fun sample(): State {
        // RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val used = mi.totalMem - mi.availMem
        val ramPct = if (mi.totalMem > 0) ((used * 100) / mi.totalMem).toInt() else 0

        // Temperature: battery sensor first, thermal zone as fallback (typical for TV boxes)
        val temp = readBatteryTemp() ?: readThermalZone()

        // Network speed: bytes since previous sample
        val now = SystemClock.elapsedRealtime()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        var down: Float? = null
        if (lastRx >= 0 && lastTx >= 0 && rx >= 0 && tx >= 0 && now > lastTs) {
            down = ((rx - lastRx) * 1000f) / (now - lastTs)
        }
        lastRx = rx
        lastTx = tx
        lastTs = now

        return State(ramPct, mi.availMem, temp, down, isVpnActive())
    }

    @Suppress("DEPRECATION")
    private fun readBatteryTemp(): Int? = runCatching {
        val intent =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val t = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        if (t != Int.MIN_VALUE && t > 0) t / 10 else null
    }.getOrNull()

    private fun readThermalZone(): Int? = runCatching {
        File("/sys/class/thermal/thermal_zone0/temp")
            .readText().trim().toIntOrNull()
            ?.takeIf { it > 0 }
            ?.let { it / 1000 }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun isVpnActive(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }.getOrDefault(false)
}
