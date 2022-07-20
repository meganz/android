package mega.privacy.android.app.data.repository

import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
import javax.inject.Inject

/**
 * Default implementation of [CameraUploadRepository]
 *
 * @property localStorageGateway MegaLocalStorageGateway
 */
class DefaultCameraUploadRepository @Inject constructor(
    private val localStorageGateway: MegaLocalStorageGateway,
) : CameraUploadRepository {

    /**
     * All different synchronization timestamps
     */
    enum class SyncTimeStamp {
        /**
         * only primary photos
         */
        PRIMARY_PHOTO,

        /**
         * primary videos
         */
        PRIMARY_VIDEO,

        /**
         * only secondary photos
         */
        SECONDARY_PHOTO,

        /**
         * secondary videos
         */
        SECONDARY_VIDEO
    }

    override fun isSyncByWifi() = localStorageGateway.isSyncByWifi()

    override fun isSyncByWifiDefault() = localStorageGateway.isSyncByWifiDefault()

    override fun getPendingSyncRecords(): List<SyncRecord> =
        localStorageGateway.getPendingSyncRecords()

    override fun setPhotosSyncFileUpload() = localStorageGateway.setPhotosSyncUpload()

    override fun getSyncFileUpload(): String? = localStorageGateway.getCameraSyncFileUpload()

    override fun getVideoQuality(): String = localStorageGateway.getVideoQuality()

    override fun deleteAllSyncRecords(syncRecordType: Int) =
        localStorageGateway.deleteAllSyncRecords(syncRecordType)

    override fun deleteSyncRecord(path: String?, isSecondary: Boolean) =
        localStorageGateway.deleteSyncRecordByPath(path, isSecondary)

    override fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean) =
        localStorageGateway.deleteSyncRecordByLocalPath(localPath, isSecondary)

    override fun deleteSyncRecordByFingerprint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    ) = localStorageGateway.deleteSyncRecordByFingerPrint(originalPrint, newPrint, isSecondary)

    override fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord? =
        localStorageGateway.getSyncRecordByFingerprint(fingerprint, isSecondary, isCopy)

    override fun getSyncRecordByNewPath(path: String): SyncRecord? =
        localStorageGateway.getSyncRecordByNewPath(path)

    override fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord? =
        localStorageGateway.getSyncRecordByLocalPath(path, isSecondary)

    override fun shouldClearSyncRecords(clearSyncRecords: Boolean) =
        localStorageGateway.shouldClearSyncRecords(clearSyncRecords)

    override fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean = localStorageGateway.doesFileNameExist(fileName, isSecondary, type)

    override fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean = localStorageGateway.doesLocalPathExist(fileName, isSecondary, type)

    override fun saveSyncRecord(record: SyncRecord) = localStorageGateway.saveSyncRecord(record)

    override fun getSyncTimeStamp(type: SyncTimeStamp): Long {
        return when (type) {
            SyncTimeStamp.PRIMARY_PHOTO -> localStorageGateway.getPhotoTimeStamp()
            SyncTimeStamp.PRIMARY_VIDEO -> localStorageGateway.getVideoTimeStamp()
            SyncTimeStamp.SECONDARY_PHOTO -> localStorageGateway.getSecondaryPhotoTimeStamp()
            SyncTimeStamp.SECONDARY_VIDEO -> localStorageGateway.getSecondaryVideoTimeStamp()
        }
    }

    override fun setSyncTimeStamp(timeStamp: Long, type: SyncTimeStamp) {
        when (type) {
            SyncTimeStamp.PRIMARY_PHOTO -> localStorageGateway.setPhotoTimeStamp(timeStamp)
            SyncTimeStamp.PRIMARY_VIDEO -> localStorageGateway.setVideoTimeStamp(timeStamp)
            SyncTimeStamp.SECONDARY_PHOTO -> localStorageGateway.setSecondaryPhotoTimeStamp(
                timeStamp)
            SyncTimeStamp.SECONDARY_VIDEO -> localStorageGateway.setSecondaryVideoTimeStamp(
                timeStamp)
        }
    }

    override fun doCredentialsExist(): Boolean = localStorageGateway.doCredentialsExist()

    override fun doPreferencesExist(): Boolean = localStorageGateway.doPreferencesExist()

    override fun isSyncEnabled(): Boolean = localStorageGateway.isSyncEnabled()

    override fun getSyncLocalPath(): String? = localStorageGateway.getSyncLocalPath()

    override fun setSyncLocalPath(localPath: String) =
        localStorageGateway.setSyncLocalPath(localPath)

    override fun setSecondaryFolderPath(secondaryFolderPath: String) =
        localStorageGateway.setSecondaryFolderPath(secondaryFolderPath)

    override fun setSecondaryEnabled(secondaryCameraUpload: Boolean) =
        localStorageGateway.setSecondaryEnabled(secondaryCameraUpload)

    override fun getSecondaryFolderPath(): String? = localStorageGateway.getSecondaryFolderPath()

    override fun getRemoveGpsDefault(): Boolean = localStorageGateway.getRemoveGpsDefault()

    override fun getUploadVideoQuality(): String? = localStorageGateway.getUploadVideoQuality()

    override fun getKeepFileNames(): Boolean = localStorageGateway.getKeepFileNames()

    override fun isFolderExternalSd(): Boolean = localStorageGateway.isFolderExternalSd()

    override fun getUriExternalSd(): String = localStorageGateway.getUriExternalSd()

    override fun isSecondaryMediaFolderEnabled() =
        localStorageGateway.isSecondaryMediaFolderEnabled()

    override fun isMediaFolderExternalSd() = localStorageGateway.isMediaFolderExternalSd()

    override fun getUriMediaFolderExternalSd(): String =
        localStorageGateway.getUriMediaFolderExternalSd()

    override fun shouldClearSyncRecords() = localStorageGateway.shouldClearSyncRecords()

    override fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long =
        localStorageGateway.getMaxTimestamp(isSecondary, syncRecordType)

    override fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord> =
        localStorageGateway.getVideoSyncRecordsByStatus(syncStatusType)

    override fun getChargingOnSizeString(): String = localStorageGateway.getChargingOnSizeString()

    override fun getChargingOnSize() = localStorageGateway.getChargingOnSizeString().toInt()

    override fun convertOnCharging() = localStorageGateway.convertOnCharging()

    override fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    ) = localStorageGateway.updateSyncRecordStatusByLocalPath(syncStatusType,
        localPath,
        isSecondary)
}
