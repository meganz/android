package mega.privacy.android.data.facade

import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import mega.privacy.android.data.gateway.FileAttributeGateway
import timber.log.Timber
import javax.inject.Inject

/**
 * File Attributes Facade implements [FileAttributeGateway]
 */
internal class FileAttributeFacade @Inject constructor() : FileAttributeGateway {

    override suspend fun getVideoGPSCoordinates(filePath: String): Pair<Float, Float> {
        var coordinates = Pair(0F, 0F)
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            if (location != null) {
                var secondTry = false
                try {
                    val mid = location.length / 2
                    val latitude = location.substring(0, mid)
                    val longitude = location.substring(mid)
                    coordinates = Pair(latitude.toFloat(), longitude.toFloat())
                } catch (ex: Exception) {
                    secondTry = true
                    Timber.e(ex)
                }
                if (secondTry) {
                    try {
                        val latitude = location.substring(0, 7)
                        val longitude = location.substring(8, 17)
                        coordinates = Pair(latitude.toFloat(), longitude.toFloat())
                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }
                }
            } else {
                Timber.w("No Video GPS coordinates found")
            }
            retriever.release()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        return coordinates
    }

    override suspend fun getPhotoGPSCoordinates(filePath: String): Pair<Float, Float> {
        var coordinates = Pair(0F, 0F)
        try {
            val exif = ExifInterface(filePath)
            val latLong = exif.latLong
            if (latLong != null) {
                coordinates = Pair(latLong[0].toFloat(), latLong[1].toFloat())
            } else {
                Timber.w("No Photo GPS coordinates found")
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        return coordinates
    }
}
