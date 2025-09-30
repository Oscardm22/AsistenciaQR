package com.example.asistenciaqr.util

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHelper(private val context: Context) {

    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    // Construir una dirección más específica y detallada
                    val sb = StringBuilder()

                    // 1. Calle y número (más específico)
                    if (address.thoroughfare != null) {
                        sb.append(address.thoroughfare)
                        if (address.subThoroughfare != null) {
                            sb.append(" #").append(address.subThoroughfare)
                        }
                        sb.append(", ")
                    }

                    // 2. Colonia/Barrio/Localidad
                    if (address.subLocality != null) {
                        sb.append(address.subLocality).append(", ")
                    } else if (address.featureName != null && address.featureName != address.thoroughfare) {
                        sb.append(address.featureName).append(", ")
                    }

                    // 3. Ciudad/Municipio
                    if (address.locality != null) {
                        sb.append(address.locality)
                    } else if (address.subAdminArea != null) {
                        sb.append(address.subAdminArea)
                    }

                    // 4. Estado (solo si no es redundante)
                    if (address.adminArea != null &&
                        (address.locality == null || address.adminArea != address.locality)) {
                        if (sb.isNotEmpty() && !sb.endsWith(", ")) sb.append(", ")
                        sb.append(address.adminArea)
                    }

                    // 5. Si no se pudo obtener nada específico, intentar con más campos
                    if (sb.isEmpty() || sb.length < 10) {
                        // Reiniciar y probar con campos alternativos
                        sb.clear()

                        // Intentar con getAddressLine que a veces da mejor resultado
                        if (address.maxAddressLineIndex >= 0) {
                            for (i in 0..minOf(2, address.maxAddressLineIndex)) {
                                val addressLine = address.getAddressLine(i)
                                if (addressLine != null && addressLine.length > 5) {
                                    if (sb.isNotEmpty()) sb.append(", ")
                                    sb.append(addressLine)
                                }
                            }
                        }

                        // Si aún está vacío, usar coordenadas formateadas
                        if (sb.isEmpty()) {
                            return@withContext "Ubicación: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
                        }
                    }

                    // Limpiar comas extras al final
                    var result = sb.toString().trim()
                    if (result.endsWith(",")) {
                        result = result.substring(0, result.length - 1)
                    }

                    return@withContext result

                } else {
                    return@withContext "Ubicación no disponible"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // En caso de error, devolver coordenadas formateadas
                return@withContext "Ubicación: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
            }
        }
    }

    // Versión alternativa que prioriza la dirección más específica posible
    suspend fun getDetailedAddressFromLocation(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 3) // Pedir más resultados

                if (!addresses.isNullOrEmpty()) {
                    // Probar con cada address hasta encontrar una buena
                    for (address in addresses) {
                        val detailedAddress = buildDetailedAddress(address)
                        if (detailedAddress.isNotEmpty() && detailedAddress.length > 10) {
                            return@withContext detailedAddress
                        }
                    }

                    // Si ninguna dirección es buena, usar la primera disponible
                    val fallbackAddress = buildDetailedAddress(addresses[0])
                    if (fallbackAddress.isNotEmpty()) {
                        return@withContext fallbackAddress
                    }
                }

                // Fallback a coordenadas
                return@withContext "Ubicación: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "Ubicación: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
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