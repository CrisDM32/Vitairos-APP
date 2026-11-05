package com.vitairos.industrialai.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed interface GpsResult {
    data class Success(val latitude: Double, val longitude: Double, val accuracyMeters: Float) : GpsResult
    data object NoPermission : GpsResult
    data object Disabled : GpsResult
    data object Timeout : GpsResult
}

/** GPS con el LocationManager nativo (sin Google Play Services). */
object LocationHelper {
    private const val FIX_TIMEOUT_MS = 12_000L

    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasPermission(context: Context) = permissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): GpsResult {
        if (!hasPermission(context)) return GpsResult.NoPermission
        val lm = context.getSystemService(LocationManager::class.java) ?: return GpsResult.Disabled
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return GpsResult.Disabled

        for (provider in providers) {
            val recent = runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            if (recent != null && System.currentTimeMillis() - recent.time < 2 * 60_000L) return recent.toSuccess()
            val fix = withTimeoutOrNull(FIX_TIMEOUT_MS) { requestFix(context, lm, provider) }
            if (fix != null) return fix.toSuccess()
        }
        return GpsResult.Timeout
    }

    private fun Location.toSuccess() = GpsResult.Success(latitude, longitude, accuracy)

    @SuppressLint("MissingPermission")
    private suspend fun requestFix(context: Context, lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                lm.getCurrentLocation(provider, signal, context.mainExecutor) { if (cont.isActive) cont.resume(it) }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) { if (cont.isActive) cont.resume(location) }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        }
}
