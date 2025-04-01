package mega.privacy.android.data.gateway

import android.content.Context
import android.net.Uri
import java.io.InputStream
import kotlin.time.Duration

/**
 * File Attributes Gateway
 */
interface FileAttributeGateway {

    /**
     * Get GPS coordinates from video file
     *
     * @param filePath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getVideoGPSCoordinates(filePath: String): Pair<Double, Double>?

    /**
     * Get GPS coordinates from photo file
     *
     * @param filePath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getPhotoGPSCoordinates(filePath: String): Pair<Double, Double>?

    /**
     * Get GPS coordinates from video file
     *
     * @param uri
     * @param context
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getVideoGPSCoordinates(uri: Uri, context: Context): Pair<Double, Double>?

    /**
     * Get GPS coordinates from photo file
     *
     * @param inputStream
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getPhotoGPSCoordinates(inputStream: InputStream): Pair<Double, Double>?

    /**
     * Get the video duration of this file or null if it's not a Video or duration can be known
     */
    suspend fun getVideoDuration(filePathOrUri: String): Duration?
}
