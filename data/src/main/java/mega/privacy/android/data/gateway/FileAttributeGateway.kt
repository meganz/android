package mega.privacy.android.data.gateway

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
     * Get the video duration of this file or null if it's not a Video or duration can be known
     */
    suspend fun getVideoDuration(filePath: String): Duration?
}
