package mega.privacy.android.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.GeocoderRepository
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Default implementation of [GeocoderRepository] backed by the Android [Geocoder].
 */
internal class GeocoderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GeocoderRepository {

    override suspend fun getAddressLine(latitude: Double, longitude: Double): String? =
        withContext(ioDispatcher) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
            address?.toAddressLine()
        }

    private fun Address.toAddressLine(): String? =
        listOfNotNull(subLocality, locality, adminArea, countryName)
            .filter { it.isNotBlank() }
            .joinToString(separator = ", ")
            .ifEmpty { null }
}
