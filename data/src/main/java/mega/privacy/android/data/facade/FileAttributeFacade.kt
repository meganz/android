package mega.privacy.android.data.facade

import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.mapper.ISO6709LocationMapper
import timber.log.Timber
import javax.inject.Inject


/**
 * File Attributes Facade implements [FileAttributeGateway]
 */
internal class FileAttributeFacade @Inject constructor(
    private val locationMapper: ISO6709LocationMapper,
) : FileAttributeGateway {

    override suspend fun getVideoGPSCoordinates(filePath: String): Pair<Double, Double>? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
        //MediaMetadataRetriever directly cannot access GPS coordinates.
        // You need to look for dedicated methods or utilize additional libraries
        // based on the file format's specifications.
        // For advanced needs, you can explore lower-level APIs like MediaCodec and MediaExtractor
        // to access raw data and extract specific information through custom parsing techniques.
        //some video formats may store location data in custom boxes not accessible by MediaMetadataRetriever.
        // Consider specialized parsers or tools based on the file format.
        retriever.release()
        return location?.let {
            locationMapper(it)
        } ?: run {
            Timber.w("No Video GPS coordinates found")
            null
        }
    }

    override suspend fun getPhotoGPSCoordinates(filePath: String): Pair<Double, Double>? {
        return runCatching {
            val exif = ExifInterface(filePath)
            val latLong = exif.latLong
            return latLong?.let {
                Pair(latLong[0], latLong[1])
            } ?: run {
                Timber.w("No Photo GPS coordinates found")
                null
            }
        }.onFailure {
            Timber.e("getPhotoGPSCoordinates Exception $it")
        }.getOrNull()
    }
}
