package mega.privacy.android.data.gateway.preferences

/**
 * CameraUploads Settings Preference Gateway
 *
 */
interface CameraUploadsSettingsPreferenceGateway {

    /**
     * Is camera uploads enabled
     */
    suspend fun isCameraUploadsEnabled(): Boolean?

    /**
     * Is media uploads enabled
     */
    suspend fun isMediaUploadsEnabled(): Boolean?

    /**
     * Get Camera Uploads handle
     */
    suspend fun getCameraUploadsHandle(): Long?

    /**
     * Get Media Uploads  handle
     */
    suspend fun getMediaUploadsHandle(): Long?

    /**
     * Set Media Uploads handle
     */
    suspend fun setMediaUploadsHandle(handle: Long?)

    /**
     * Set Camera Uploads handle
     */
    suspend fun setCameraUploadsHandle(handle: Long?)

    /**
     * Retrieves the Camera Uploads Folder local path
     *
     * @return A [String] that contains the Camera Uploads Folder local path
     */
    suspend fun getCameraUploadsLocalPath(): String?

    /**
     * Set Camera Uploads Folder local path
     *
     * @param path
     */
    suspend fun setCameraUploadsLocalPath(path: String?)

    /**
     * Retrieves the Secondary Folder local path
     *
     * @return A [String] that contains the Primary Folder local path
     */
    suspend fun getMediaUploadsLocalPath(): String?

    /**
     * Sets the new Media Uploads Folder local path
     *
     * @param path The new Media Uploads Folder local path
     */
    suspend fun setMediaUploadsLocalPath(path: String?)

    /**
     * Set Camera uploads enabled
     */
    suspend fun setCameraUploadsEnabled(isEnabled: Boolean)

    /**
     * Set Media uploads enabled
     */
    suspend fun setMediaUploadsEnabled(isEnabled: Boolean)

    /**
     * Checks the value in the Database, as to whether Location Tags should be added or not
     * when uploading Photos
     *
     * @return true if Location Tags should be added when uploading Photos
     */
    suspend fun areLocationTagsEnabled(): Boolean?

    /**
     * Sets the new value in the Database, as to whether Location Tags should be added or not
     * when uploading Photos
     *
     * @param isEnabled true if Location Tags should be added when uploading Photos
     */
    suspend fun setLocationTagsEnabled(isEnabled: Boolean)


    /**
     * Get video quality for camera upload
     */
    suspend fun getUploadVideoQuality(): Int?

    /**
     * Sets the new Video Quality when uploading Videos through Camera Uploads
     *
     * @param quality The Video Quality, represented as an [Int]
     */
    suspend fun setUploadVideoQuality(quality: Int)

    /**
     * Sets whether the File Names of files to be uploaded will be kept or not
     *
     * @param keepFileNames true if the File Names should now be left as is
     */
    suspend fun setUploadFileNamesKept(keepFileNames: Boolean)


    /**
     * Checks whether the File Names are kept or not when uploading content
     *
     * @return true if the File Names should be left as is
     */
    suspend fun areUploadFileNamesKept(): Boolean?

    /**
     * Checks whether compressing videos require the device to be charged or not
     *
     * @return true if the device needs to be charged to compress videos
     */
    suspend fun isChargingRequiredForVideoCompression(): Boolean?

    /**
     * Sets whether compressing videos require the device to be charged or not
     *
     * @param chargingRequired Whether the device needs to be charged or not
     */
    suspend fun setChargingRequiredForVideoCompression(chargingRequired: Boolean)

    /**
     * Retrieves the maximum video file size that can be compressed
     *
     * @return An [Int] that represents the maximum video file size that can be compressed
     */
    suspend fun getVideoCompressionSizeLimit(): Int?

    /**
     * Sets the maximum video file size that can be compressed
     *
     * @param size The maximum video file size that can be compressed
     */
    suspend fun setVideoCompressionSizeLimit(size: Int)

    /**
     * Get file upload option
     */
    suspend fun getFileUploadOption(): Int?

    /**
     * Sets the upload option of Camera Uploads
     *
     * @param uploadOption A specific [Int] from MegaPreferences
     */
    suspend fun setFileUploadOption(uploadOption: Int)

    /**
     * Checks if content in Camera Uploads should be uploaded through Wi-Fi only,
     * or through Wi-Fi or Mobile Data
     *
     * @return If true, will only upload on Wi-Fi.
     * if false, will upload through Wi-Fi or Mobile Data
     */
    suspend fun isUploadByWifi(): Boolean?

    /**
     * Sets whether Camera Uploads should only run through Wi-Fi / Wi-Fi or Mobile Data
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    suspend fun setUploadsByWifi(wifiOnly: Boolean)

    /**
     * Clear preferences
     */
    suspend fun clearPreferences()
}
