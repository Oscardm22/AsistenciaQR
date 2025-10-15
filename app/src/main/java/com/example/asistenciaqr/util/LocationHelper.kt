package com.example.asistenciaqr.util

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHelper(private val context: Context) {

    suspend fun getDetailedAddressFromLocation(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 3)

                if (!addresses.isNullOrEmpty()) {
                    for (address in addresses) {
                        val detailedAddress = buildDetailedAddress(address)
                        if (detailedAddress.isNotEmpty() && detailedAddress.length > 10) {
                            return@withContext detailedAddress
                        }
                    }

                    val fallbackAddress = buildDetailedAddress(addresses[0])
                    if (fallbackAddress.isNotEmpty()) {
                        return@withContext fallbackAddress
                    }
                }

                // Fallback a coordenadas
                return@withContext String.format(
                    Locale.getDefault(),
                    "Ubicación: %.6f, %.6f",
                    latitude,
                    longitude
                )

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext String.format(
                    Locale.getDefault(),
                    "Ubicación: %.6f, %.6f",
                    latitude,
                    longitude
                )
            }
        }
    }

    private fun buildDetailedAddress(address: android.location.Address): String {
        val sb = StringBuilder()

        // Prioridad 1: Calle y número
        if (address.thoroughfare != null) {
            sb.append(address.thoroughfare)
            if (address.subThoroughfare != null) {
                sb.append(" #").append(address.subThoroughfare)
            }
        }

        // Prioridad 2: Colonia/Barrio
        if (address.subLocality != null) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(address.subLocality)
        }

        // Prioridad 3: Ciudad
        if (address.locality != null) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(address.locality)
        }

        // Solo agregar estado si es necesario y no es redundante
        if (address.adminArea != null &&
            address.locality != address.adminArea &&
            !sb.contains(address.adminArea)) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(address.adminArea)
        }

        return sb.toString()
    }
}