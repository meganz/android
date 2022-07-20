package mega.privacy.android.app.data.facade

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.model.UserCredentials
import mega.privacy.android.domain.entity.SyncRecord
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import javax.inject.Inject

/**
 * Mega preferences facade
 *
 * Implements [MegaLocalStorageGateway] and provides a facade over [DatabaseHandler]
 *
 * @property dbHandler
 */
class MegaLocalStorageFacade @Inject constructor(
    val dbHandler: DatabaseHandler,
) : MegaLocalStorageGateway {

    override suspend fun getCamSyncHandle(): Long? =
        dbHandler.preferences?.camSyncHandle?.toLongOrNull()

    override suspend fun getMegaHandleSecondaryFolder(): Long? =
        dbHandler.preferences?.megaHandleSecondaryFolder?.toLongOrNull()

    override suspend fun getCloudSortOrder(): Int =
        dbHandler.preferences?.preferredSortCloud?.toInt() ?: ORDER_DEFAULT_ASC

    override suspend fun getCameraSortOrder(): Int =
        dbHandler.preferences?.preferredSortCameraUpload?.toInt() ?: ORDER_MODIFICATION_DESC

    override suspend fun getUserCredentials(): UserCredentials? = dbHandler.credentials

    override fun isSyncByWifi(): Boolean = dbHandler.preferences?.camSyncWifi.toBoolean()

    override fun isSyncByWifiDefault(): Boolean =
        dbHandler.preferences?.camSyncWifi?.toBoolean() ?: true

    override fun getPendingSyncRecords(): List<SyncRecord> = dbHandler.findAllPendingSyncRecords()

    override fun setPhotosSyncUpload() = dbHandler.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS)

    override fun getCameraSyncFileUpload(): String? = dbHandler.preferences?.camSyncFileUpload

    override fun getVideoQuality(): String = dbHandler.preferences.uploadVideoQuality

    override fun deleteAllSyncRecords(syncRecordType: Int) =
        dbHandler.deleteAllSyncRecords(syncRecordType)

    override fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean) =
        dbHandler.deleteSyncRecordByPath(path, isSecondary)

    override fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean) =
        dbHandler.deleteSyncRecordByLocalPath(localPath, isSecondary)

    override fun deleteSyncRecordByFingerPrint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    ) = dbHandler.deleteSyncRecordByFingerprint(originalPrint, newPrint, isSecondary)

    override fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord? = dbHandler.recordExists(fingerprint, isSecondary, isCopy)

    override fun getSyncRecordByNewPath(path: String): SyncRecord? =
        dbHandler.findSyncRecordByNewPath(path)

    override fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord? =
        dbHandler.findSyncRecordByLocalPath(path, isSecondary)

    override fun shouldClearSyncRecords(clearSyncRecords: Boolean) =
        dbHandler.saveShouldClearCamsyncRecords(clearSyncRecords)

    override fun doesFileNameExist(fileName: String, isSecondary: Boolean, type: Int): Boolean =
        dbHandler.fileNameExists(fileName, isSecondary, type)

    override fun doesLocalPathExist(fileName: String, isSecondary: Boolean, type: Int): Boolean =
        dbHandler.localPathExists(fileName, isSecondary, type)

    override fun saveSyncRecord(record: SyncRecord) = dbHandler.saveSyncRecord(record)

    override fun getPhotoTimeStamp(): Long =
        dbHandler.preferences?.camSyncTimeStamp?.toLongOrNull() ?: 0

    override fun getSecondaryPhotoTimeStamp(): Long =
        dbHandler.preferences?.secSyncTimeStamp?.toLongOrNull() ?: 0

    override fun getVideoTimeStamp(): Long =
        dbHandler.preferences?.camVideoSyncTimeStamp?.toLongOrNull() ?: 0

    override fun getSecondaryVideoTimeStamp(): Long =
        dbHandler.preferences?.secVideoSyncTimeStamp?.toLongOrNull() ?: 0

    override fun setPhotoTimeStamp(timeStamp: Long) = dbHandler.setCamSyncTimeStamp(timeStamp)

    override fun setSecondaryPhotoTimeStamp(timeStamp: Long) =
        dbHandler.setSecSyncTimeStamp(timeStamp)

    override fun setVideoTimeStamp(timeStamp: Long) = dbHandler.setCamVideoSyncTimeStamp(timeStamp)

    override fun setSecondaryVideoTimeStamp(timeStamp: Long) =
        dbHandler.setSecVideoSyncTimeStamp(timeStamp)

    override fun doCredentialsExist(): Boolean = dbHandler.credentials != null

    override fun doPreferencesExist(): Boolean = dbHandler.preferences != null

    override fun isSyncEnabled(): Boolean =
        dbHandler.preferences.camSyncEnabled.toBoolean()

    override fun getSyncLocalPath(): String? =
        dbHandler.preferences.camSyncLocalPath

    override fun setSyncLocalPath(localPath: String) =
        dbHandler.setCamSyncLocalPath(localPath)

    override fun setSecondaryFolderPath(secondaryFolderPath: String) =
        dbHandler.setSecondaryFolderPath(secondaryFolderPath)

    override fun setSecondaryEnabled(secondaryCameraUpload: Boolean) =
        dbHandler.setSecondaryUploadEnabled(secondaryCameraUpload)

    override fun getSecondaryFolderPath(): String? =
        dbHandler.preferences.localPathSecondaryFolder

    override fun getRemoveGpsDefault(): Boolean =
        dbHandler.preferences.removeGPS?.toBoolean() ?: true

    override fun getUploadVideoQuality(): String? = dbHandler.preferences.uploadVideoQuality

    override fun getKeepFileNames(): Boolean = dbHandler.preferences.keepFileNames.toBoolean()

    override fun isFolderExternalSd(): Boolean =
        dbHandler.preferences.cameraFolderExternalSDCard.toBoolean()

    override fun getUriExternalSd(): String = dbHandler.preferences.uriExternalSDCard

    override fun isSecondaryMediaFolderEnabled(): Boolean =
        dbHandler.preferences.keepFileNames.toBoolean()

    override fun isMediaFolderExternalSd(): Boolean = dbHandler.mediaFolderExternalSdCard

    override fun getUriMediaFolderExternalSd(): String = dbHandler.uriMediaExternalSdCard

    override fun shouldClearSyncRecords(): Boolean = dbHandler.shouldClearCamsyncRecords()

    override fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long =
        dbHandler.findMaxTimestamp(isSecondary, syncRecordType) ?: 0

    override fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord> =
        dbHandler.findVideoSyncRecordsByState(syncStatusType)

    override fun getChargingOnSizeString(): String = dbHandler.preferences.chargingOnSize

    override fun convertOnCharging(): Boolean =
        doPreferencesExist() && dbHandler.preferences.conversionOnCharging.toBoolean()

    override fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    ) = dbHandler.updateSyncRecordStatusByLocalPath(syncStatusType, localPath, isSecondary)
}
