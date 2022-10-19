package mega.privacy.android.data.gateway.preferences

/**
 * Last Camera Timestamps Preference Gateway
 */
interface CameraTimestampsPreferenceGateway {
    /**
     * Backup time stamps, primary upload folder and secondary folder in share preference after
     * database records being cleaned
     * @param primaryUploadFolderHandle
     * @param secondaryUploadFolderHandle
     * @param camSyncTimeStamp
     * @param camVideoSyncTimeStamp
     * @param secSyncTimeStamp
     * @param secVideoSyncTimeStamp
     */
    suspend fun backupTimestampsAndFolderHandle(
        primaryUploadFolderHandle: Long,
        secondaryUploadFolderHandle: Long,
        camSyncTimeStamp: String?,
        camVideoSyncTimeStamp: String?,
        secSyncTimeStamp: String?,
        secVideoSyncTimeStamp: String?,
    )


    /**
     * @return [Long] primary handle
     */
    suspend fun getPrimaryHandle(): Long?

    /**
     * @return [Long] secondary handle
     */
    suspend fun getSecondaryHandle(): Long?

    /**
     * @return [String] primary folder photo sync timestamp
     */
    suspend fun getPrimaryFolderPhotoSyncTime(): String?

    /**
     * @return [String] secondary folder photo sync timestamp
     */
    suspend fun getSecondaryFolderPhotoSyncTime(): String?

    /**
     * @return [String] primary folder video sync timestamp
     */
    suspend fun getPrimaryFolderVideoSyncTime(): String?

    /**
     * @return [String] secondary folder video sync timestamp
     */
    suspend fun getSecondaryFolderVideoSyncTime(): String?

    /**
     * Clear Primary Sync Records from Preference
     */
    suspend fun clearPrimaryCameraSyncRecords()

    /**
     * Clear Secondary Sync Records from Preference
     */
    suspend fun clearSecondaryCameraSyncRecords()
}
