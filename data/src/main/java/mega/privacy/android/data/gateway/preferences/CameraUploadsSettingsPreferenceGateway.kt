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
     * @param localPath The new Media Uploads Folder local path
     */
    suspend fun setMediaUploadsLocalPath(localPath: String?)

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
     * @param enable true if Location Tags should be added when uploading Photos
     */
    suspend fun setLocationTagsEnabled(enable: Boolean)


    /**
     * Get video quality for camera upload
     */
    suspend fun getUploadVideoQuality(): String?

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
    suspend fun getFileUploadOption(): String?

    /**
     * Sets the upload option of Camera Uploads
     *
     * @param uploadOption A specific [Int] from MegaPreferences
     */
    suspend fun setFileUploadOption(uploadOption: Int)

    /**
     * Get photo time stamp
     */
    suspend fun getPhotoTimeStamp(): String?

    /**
     * Set photo time stamp
     */
    suspend fun setPhotoTimeStamp(timeStamp: Long)

    /**
     * Get video time stamp
     */
    suspend fun getVideoTimeStamp(): String?

    /**
     * Set video time stamp
     */
    suspend fun setVideoTimeStamp(timeStamp: Long)

    /**
     * Get media uploads photo time stamp
     */
    suspend fun getMediaUploadsPhotoTimeStamp(): String?

    /**
     * Set secondary photo time stamp
     */
    suspend fun setMediaUploadsPhotoTimeStamp(timeStamp: Long)

    /**
     * Get Media Uploads video time stamp
     */
    suspend fun getMediaUploadsVideoTimeStamp(): String?

    /**
     * Set Media Uploads video time stamp
     */
    suspend fun setMediaUploadsVideoTimeStamp(timeStamp: Long)

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
    suspend fun clear()
}
