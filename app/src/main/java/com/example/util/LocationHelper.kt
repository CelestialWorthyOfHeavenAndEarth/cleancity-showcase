package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.data.DemoScenario
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationData(val latitude: Double, val longitude: Double, val address: String, val isSample: Boolean = false)
object LocationHelper {
    private fun unavailable() = LocationData(DemoScenario.latitude, DemoScenario.longitude, DemoScenario.address, isSample = true)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationData {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return unavailable()
        val coordinates = suspendCancellableCoroutine<Location?> { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }
            fun finish(location: Location?) { if (continuation.isActive) continuation.resume(location) }
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.getCurrentLocation(if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                    .addOnSuccessListener { current ->
                        if (current != null) finish(current)
                        else client.lastLocation.addOnSuccessListener { finish(it) }.addOnFailureListener { finish(null) }
                    }.addOnFailureListener { finish(null) }
            } catch (_: Exception) { finish(null) }
        } ?: return unavailable()
        // Geocoding may block on some Android versions, so it never runs on the UI thread.
        return withContext(Dispatchers.IO) {
            LocationData(coordinates.latitude, coordinates.longitude, getReadableAddress(context, coordinates.latitude, coordinates.longitude))
        }
    }
    @Suppress("DEPRECATION")
    fun getReadableAddress(context: Context, lat: Double, lng: Double): String {
        val fallback = "%.4f, %.4f".format(Locale.US, lat, lng)
        return try {
            val result = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)?.firstOrNull()
            result?.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) { fallback }
    }
}

