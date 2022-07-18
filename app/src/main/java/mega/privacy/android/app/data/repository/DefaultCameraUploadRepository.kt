package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Inject

/**
 * Default implementation of [CameraUploadRepository]
 *
 * @property databaseHandler DatabaseHandler
 * @property context Context
 * @property threadPoolExecutor ThreadPoolExecutor
 * @property ioDispatcher CoroutineDispatcher
 * @property megaLocalStorageFacade MegaLocalStorageGateway
 */
class DefaultCameraUploadRepository @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
    private val threadPoolExecutor: ThreadPoolExecutor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageFacade: MegaLocalStorageGateway,
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

    override fun isSyncByWifi() =
        databaseHandler.preferences.camSyncWifi.toBoolean()

    override fun isSyncByWifiDefault(): Boolean =
        databaseHandler.preferences.camSyncWifi?.toBoolean() ?: true

    override fun getPendingSyncRecords(): List<SyncRecord> =
        databaseHandler.findAllPendingSyncRecords()

    override fun setPhotosSyncFileUpload() =
        databaseHandler.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS)

    override fun getSyncFileUpload(): String? = databaseHandler.preferences.camSyncFileUpload

    override fun getVideoQuality(): String = databaseHandler.preferences.uploadVideoQuality

    override fun deleteAllSyncRecords(syncRecordType: Int) =
        databaseHandler.deleteAllSyncRecords(syncRecordType)

    override fun deleteSyncRecord(path: String?, isSecondary: Boolean) =
        databaseHandler.deleteSyncRecordByPath(path, isSecondary)

    override fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean) =
        databaseHandler.deleteSyncRecordByLocalPath(localPath, isSecondary)

    override fun deleteSyncRecordByFingerprint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    ) =
        databaseHandler.deleteSyncRecordByFingerprint(originalPrint, newPrint, isSecondary)

    override fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord? = databaseHandler.recordExists(fingerprint, isSecondary, isCopy)

    override fun getSyncRecordByNewPath(path: String): SyncRecord? =
        databaseHandler.findSyncRecordByNewPath(path)

    override fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord? =
        databaseHandler.findSyncRecordByLocalPath(path, isSecondary)

    override fun shouldClearSyncRecords(clearSyncRecords: Boolean) =
        databaseHandler.saveShouldClearCamsyncRecords(clearSyncRecords)

    override fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean = databaseHandler.fileNameExists(fileName, isSecondary, type)

    override fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean = databaseHandler.localPathExists(fileName, isSecondary, type)

    override fun saveSyncRecord(record: SyncRecord) = databaseHandler.saveSyncRecord(record)

    override fun getSyncTimeStamp(type: SyncTimeStamp): Long {
        return when (type) {
            SyncTimeStamp.PRIMARY_PHOTO ->
                databaseHandler.preferences.camSyncTimeStamp?.toLongOrNull() ?: 0
            SyncTimeStamp.PRIMARY_VIDEO ->
                databaseHandler.preferences.camVideoSyncTimeStamp?.toLongOrNull() ?: 0
            SyncTimeStamp.SECONDARY_PHOTO ->
                databaseHandler.preferences.secSyncTimeStamp?.toLongOrNull() ?: 0
            SyncTimeStamp.SECONDARY_VIDEO ->
                databaseHandler.preferences.secVideoSyncTimeStamp?.toLongOrNull() ?: 0
        }
    }

    override fun setSyncTimeStamp(timestamp: Long, type: SyncTimeStamp) {
        when (type) {
            SyncTimeStamp.PRIMARY_PHOTO -> databaseHandler.setCamSyncTimeStamp(timestamp)
            SyncTimeStamp.PRIMARY_VIDEO -> databaseHandler.setCamVideoSyncTimeStamp(timestamp)
            SyncTimeStamp.SECONDARY_PHOTO -> databaseHandler.setSecSyncTimeStamp(timestamp)
            SyncTimeStamp.SECONDARY_VIDEO -> databaseHandler.setSecVideoSyncTimeStamp(timestamp)
        }
    }

    override fun doCredentialsExist(): Boolean = databaseHandler.credentials != null

    override fun doPreferencesExist(): Boolean = databaseHandler.preferences != null

    override fun isSyncEnabled() =
        databaseHandler.preferences.camSyncEnabled.toBoolean()

    override fun getSyncLocalPath(): String? =
        databaseHandler.preferences.camSyncLocalPath

    override fun setSyncLocalPath(localPath: String) =
        databaseHandler.setCamSyncLocalPath(localPath)

    override fun setSecondaryFolderPath(secondaryFolderPath: String) =
        databaseHandler.setSecondaryFolderPath(secondaryFolderPath)

    override fun setSecondaryEnabled(secondaryCameraUpload: Boolean) =
        databaseHandler.setSecondaryUploadEnabled(secondaryCameraUpload)

    override fun getSecondaryFolderPath(): String? =
        databaseHandler.preferences.localPathSecondaryFolder

    override fun getRemoveGpsDefault() =
        databaseHandler.preferences.removeGPS?.toBoolean() ?: true

    override fun getUploadVideoQuality(): String? = databaseHandler.preferences.uploadVideoQuality

    override fun getKeepFileNames() = databaseHandler.preferences.keepFileNames.toBoolean()

    override fun isFolderExternalSd() =
        databaseHandler.preferences.cameraFolderExternalSDCard.toBoolean()

    override fun getUriExternalSd(): String = databaseHandler.preferences.uriExternalSDCard

    override fun isSecondaryMediaFolderEnabled() =
        databaseHandler.preferences.keepFileNames.toBoolean()

    override fun isMediaFolderExternalSd() = databaseHandler.mediaFolderExternalSdCard

    override fun getUriMediaFolderExternalSd(): String = databaseHandler.uriMediaExternalSdCard

    override fun shouldClearSyncRecords() = databaseHandler.shouldClearCamsyncRecords()

    override fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long =
        databaseHandler.findMaxTimestamp(isSecondary, syncRecordType) ?: 0

    override fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord> =
        databaseHandler.findVideoSyncRecordsByState(syncStatusType)

    override fun getChargingOnSizeString(): String = databaseHandler.preferences.chargingOnSize

    override fun getChargingOnSize() = databaseHandler.preferences.chargingOnSize.toInt()

    override fun convertOnCharging() =
        doPreferencesExist() && databaseHandler.preferences.conversionOnCharging.toBoolean()

    override fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    ) = databaseHandler.updateSyncRecordStatusByLocalPath(syncStatusType, localPath, isSecondary)

}
