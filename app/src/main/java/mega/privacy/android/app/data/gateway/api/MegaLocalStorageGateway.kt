package mega.privacy.android.app.data.gateway.api

import mega.privacy.android.app.MegaAttributes
import mega.privacy.android.app.MegaContactDB
import mega.privacy.android.app.data.model.UserCredentials
import mega.privacy.android.app.main.megachat.NonContactInfo
import mega.privacy.android.domain.entity.SyncRecord

/**
 * MegaDBHandlerGateway gateway
 *
 * The gateway interface to the Mega DB handler functionality
 */
interface MegaLocalStorageGateway {

    /**
     * Get Camera Uploads Primary handle
     */
    suspend fun getCamSyncHandle(): Long?

    /**
     * Get Camera Uploads Secondary handle
     */
    suspend fun getMegaHandleSecondaryFolder(): Long?

    /**
     * Set Camera Uploads Primary handle
     */
    suspend fun setCamSyncHandle(primaryHandle: Long)

    /**
     * Set Camera Uploads Secondary handle
     */
    suspend fun setMegaHandleSecondaryFolder(secondaryHandle: Long)

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend fun getCloudSortOrder(): Int

    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend fun getCameraSortOrder(): Int

    /**
     * Get others sort order
     * @return others sort order
     */
    suspend fun getOthersSortOrder(): Int

    /**
     * Get links sort order
     * @return links sort order
     */
    suspend fun getLinksSortOrder(): Int

    /**
     * Get user credentials
     *
     * @return user credentials or null
     */
    suspend fun getUserCredentials(): UserCredentials?

    /**
     * Is sync by wifi only
     * @return if sync is by wifi only
     */
    suspend fun isSyncByWifi(): Boolean

    /**
     * Is sync by wifi default
     * @return if sync is by wifi default
     */
    suspend fun isSyncByWifiDefault(): Boolean

    /**
     * Get all pending sync records
     * @return pending sync records
     */
    suspend fun getPendingSyncRecords(): List<SyncRecord>

    /**
     * Set photos sync upload
     */
    suspend fun setPhotosSyncUpload()

    /**
     * Get sync file upload
     */
    suspend fun getCameraSyncFileUpload(): String?

    /**
     * Get video quality
     */
    suspend fun getVideoQuality(): String

    /**
     * Delete sync records by type
     */
    suspend fun deleteAllSyncRecords(syncRecordType: Int)

    /**
     * Delete sync records by path
     */
    suspend fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean)

    /**
     * Delete sync records by local path
     */
    suspend fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean)

    /**
     * Delete sync records by fingerprint
     */
    suspend fun deleteSyncRecordByFingerPrint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    )

    /**
     * Get sync record by fingerprint
     * @return sync record
     */
    suspend fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord?

    /**
     * Get sync record by new path
     * @return sync record
     */
    suspend fun getSyncRecordByNewPath(path: String): SyncRecord?

    /**
     * Get sync record by local path
     * @return sync record
     */
    suspend fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord?

    /**
     * Should clear sync records
     */
    suspend fun shouldClearSyncRecords(clearSyncRecords: Boolean)

    /**
     * Does file name exist
     */
    suspend fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean

    /**
     * Does local path exist
     */
    suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean

    /**
     * Save sync record
     */
    suspend fun saveSyncRecord(record: SyncRecord)

    /**
     * Get photo time stamp
     */
    suspend fun getPhotoTimeStamp(): Long

    /**
     * Get video time stamp
     */
    suspend fun getVideoTimeStamp(): Long

    /**
     * Get secondary photo time stamp
     */
    suspend fun getSecondaryPhotoTimeStamp(): Long

    /**
     * Get secondary video time stamp
     */
    suspend fun getSecondaryVideoTimeStamp(): Long

    /**
     * Set photo time stamp
     */
    suspend fun setPhotoTimeStamp(timeStamp: Long)

    /**
     * Set video time stamp
     */
    suspend fun setVideoTimeStamp(timeStamp: Long)

    /**
     * Set secondary photo time stamp
     */
    suspend fun setSecondaryPhotoTimeStamp(timeStamp: Long)

    /**
     * Set secondary video time stamp
     */
    suspend fun setSecondaryVideoTimeStamp(timeStamp: Long)

    /**
     * Do user credentials exist
     */
    suspend fun doCredentialsExist(): Boolean

    /**
     * Do user preferences exist
     */
    suspend fun doPreferencesExist(): Boolean

    /**
     * Is camera upload sync enabled
     */
    suspend fun isSyncEnabled(): Boolean

    /**
     * Get camera upload sync local path
     */
    suspend fun getSyncLocalPath(): String?

    /**
     * Set camera upload sync local path
     */
    suspend fun setSyncLocalPath(localPath: String)

    /**
     * Set secondary folder path
     */
    suspend fun setSecondaryFolderPath(secondaryFolderPath: String)

    /**
     * Set secondary upload enabled
     */
    suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean)

    /**
     * Get secondary upload folder path
     */
    suspend fun getSecondaryFolderPath(): String?

    /**
     * Get remove GPS default setting
     */
    suspend fun getRemoveGpsDefault(): Boolean

    /**
     * Get video quality for camera upload
     */
    suspend fun getUploadVideoQuality(): String?

    /**
     * Keep file names for camera upload or not
     */
    suspend fun getKeepFileNames(): Boolean

    /**
     * Is folder path on external SD card
     */
    suspend fun isFolderExternalSd(): Boolean

    /**
     * Get external SD card URI
     */
    suspend fun getUriExternalSd(): String?

    /**
     * Is secondary media folder enabled
     */
    suspend fun isSecondaryMediaFolderEnabled(): Boolean

    /**
     * Is media upload folder path on external SD card
     */
    suspend fun isMediaFolderExternalSd(): Boolean

    /**
     * Get media folder external SD card URI
     */
    suspend fun getUriMediaFolderExternalSd(): String?

    /**
     * Should clear sync records
     */
    suspend fun shouldClearSyncRecords(): Boolean

    /**
     * Get maximum time stamp by upload sync record type
     */
    suspend fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long

    /**
     * Get video sync record with status type
     */
    suspend fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord>

    /**
     * Get charging on size string
     */
    suspend fun getChargingOnSizeString(): String

    /**
     * Convert on charging or not
     */
    suspend fun convertOnCharging(): Boolean

    /**
     * Update sync record status by local path
     */
    suspend fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    )

    /**
     * Get non contact by handle
     *
     * @param userHandle
     */
    suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo?

    /**
     * Set non contact email
     *
     * @param userHandle
     * @param email
     */
    suspend fun setNonContactEmail(userHandle: Long, email: String)


    /**
     * Get contact by email
     *
     * @param email
     * @return local contact details if found
     */
    suspend fun getContactByEmail(email: String?): MegaContactDB?

    /**
     * Set first time
     *
     * @param isFirstTime
     */
    suspend fun setUserHasLoggedIn()

    /**
     * Set to always ask for storage
     *
     * @param isStorageAskAlways
     */
    suspend fun setStorageAskAlways(isStorageAskAlways: Boolean)

    /**
     * Set storage download location
     *
     * @param storageDownloadLocation
     */
    suspend fun setStorageDownloadLocation(storageDownloadLocation: String)

    /**
     * Set passcode l ock enabled
     *
     * @param isPasscodeLockEnabled
     */
    fun setPasscodeLockEnabled(isPasscodeLockEnabled: Boolean)

    /**
     * Set the passcode lock code
     *
     * @param passcodeLockCode
     */
    suspend fun setPasscodeLockCode(passcodeLockCode: String)

    /**
     * Set show copyright
     *
     * @param showCopyrights
     */
    suspend fun setShowCopyright(showCopyrights: Boolean)

    /**
     * Set Camera Upload local path
     *
     * @param path
     */
    suspend fun setCamSyncLocalPath(path: String?)

    /**
     * Set if camera upload local path folder is on an external sd card
     *
     * @param cameraFolderExternalSDCard
     */
    suspend fun setCameraFolderExternalSDCard(cameraFolderExternalSDCard: Boolean)

    /**
     * Set if camera upload using wifi only or with cellular also
     *
     * @param enableCellularSync
     */
    suspend fun setCamSyncWifi(enableCellularSync: Boolean)

    /**
     * Set if camera upload only uploads images or images and videos
     *
     * @param fileUpload
     */
    suspend fun setCamSyncFileUpload(fileUpload: Int)

    /**
     * Set Video upload quality
     *
     * @param quality
     */
    suspend fun setCameraUploadVideoQuality(quality: Int)

    /**
     * Set Conversion on charging
     *
     * @param onCharging
     */
    suspend fun setConversionOnCharging(onCharging: Boolean)

    /**
     * Set the size of Charging on
     *
     * @param size
     */
    suspend fun setChargingOnSize(size: Int)

    /**
     * Set camera upload on/off
     *
     * @param enable
     */
    suspend fun setCamSyncEnabled(enable: Boolean)

    /**
     * Gets attributes from DB
     */
    suspend fun getAttributes(): MegaAttributes?

    /**
     * Gets pricing timestamp.
     */
    suspend fun getPricingTimeStamp(): String?

    /**
     * Gets payment methods timestamp
     */
    suspend fun getPaymentMethodsTimeStamp(): String?
}
