package mega.privacy.android.app.data.facade

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.constants.SettingsConstants.DEFAULT_CONVENTION_QUEUE_SIZE
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.model.UserCredentials
import mega.privacy.android.app.main.megachat.NonContactInfo
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.VideoQuality
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_LINK_CREATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_LINK_CREATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
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

    override suspend fun getOthersSortOrder(): Int =
        dbHandler.preferences?.preferredSortOthers?.toInt() ?: ORDER_DEFAULT_ASC

    override suspend fun getLinksSortOrder(): Int =
        when (val order = getCloudSortOrder()) {
            ORDER_MODIFICATION_ASC -> ORDER_LINK_CREATION_ASC
            ORDER_MODIFICATION_DESC -> ORDER_LINK_CREATION_DESC
            else -> order
        }


    override suspend fun getUserCredentials(): UserCredentials? = dbHandler.credentials

    override suspend fun isSyncByWifi(): Boolean = dbHandler.preferences?.camSyncWifi.toBoolean()

    override suspend fun isSyncByWifiDefault(): Boolean =
        dbHandler.preferences?.camSyncWifi?.toBoolean() ?: true

    override suspend fun getPendingSyncRecords(): List<SyncRecord> =
        dbHandler.findAllPendingSyncRecords()

    override suspend fun setPhotosSyncUpload() =
        dbHandler.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS)

    override suspend fun getCameraSyncFileUpload(): String? =
        dbHandler.preferences?.camSyncFileUpload

    override suspend fun getVideoQuality(): String =
        dbHandler.preferences?.uploadVideoQuality ?: VideoQuality.ORIGINAL.value.toString()

    override suspend fun deleteAllSyncRecords(syncRecordType: Int) =
        dbHandler.deleteAllSyncRecords(syncRecordType)

    override suspend fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean) =
        dbHandler.deleteSyncRecordByPath(path, isSecondary)

    override suspend fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean) =
        dbHandler.deleteSyncRecordByLocalPath(localPath, isSecondary)

    override suspend fun deleteSyncRecordByFingerPrint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    ) = dbHandler.deleteSyncRecordByFingerprint(originalPrint, newPrint, isSecondary)

    override suspend fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord? = dbHandler.recordExists(fingerprint, isSecondary, isCopy)

    override suspend fun getSyncRecordByNewPath(path: String): SyncRecord? =
        dbHandler.findSyncRecordByNewPath(path)

    override suspend fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord? =
        dbHandler.findSyncRecordByLocalPath(path, isSecondary)

    override suspend fun shouldClearSyncRecords(clearSyncRecords: Boolean) =
        dbHandler.saveShouldClearCamsyncRecords(clearSyncRecords)

    override suspend fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean =
        dbHandler.fileNameExists(fileName, isSecondary, type)

    override suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean =
        dbHandler.localPathExists(fileName, isSecondary, type)

    override suspend fun saveSyncRecord(record: SyncRecord) = dbHandler.saveSyncRecord(record)

    override suspend fun getPhotoTimeStamp(): Long =
        dbHandler.preferences?.camSyncTimeStamp?.toLongOrNull() ?: 0

    override suspend fun getSecondaryPhotoTimeStamp(): Long =
        dbHandler.preferences?.secSyncTimeStamp?.toLongOrNull() ?: 0

    override suspend fun getVideoTimeStamp(): Long =
        dbHandler.preferences?.camVideoSyncTimeStamp?.toLongOrNull() ?: 0

    override suspend fun getSecondaryVideoTimeStamp(): Long =
        dbHandler.preferences?.secVideoSyncTimeStamp?.toLongOrNull() ?: 0

    override suspend fun setPhotoTimeStamp(timeStamp: Long) =
        dbHandler.setCamSyncTimeStamp(timeStamp)

    override suspend fun setSecondaryPhotoTimeStamp(timeStamp: Long) =
        dbHandler.setSecSyncTimeStamp(timeStamp)

    override suspend fun setVideoTimeStamp(timeStamp: Long) =
        dbHandler.setCamVideoSyncTimeStamp(timeStamp)

    override suspend fun setSecondaryVideoTimeStamp(timeStamp: Long) =
        dbHandler.setSecVideoSyncTimeStamp(timeStamp)

    override suspend fun doCredentialsExist(): Boolean = dbHandler.credentials != null

    override suspend fun doPreferencesExist(): Boolean = dbHandler.preferences != null

    override suspend fun isSyncEnabled(): Boolean =
        dbHandler.preferences?.camSyncEnabled.toBoolean()

    override suspend fun getSyncLocalPath(): String? =
        dbHandler.preferences?.camSyncLocalPath

    override suspend fun setSyncLocalPath(localPath: String) =
        dbHandler.setCamSyncLocalPath(localPath)

    override suspend fun setSecondaryFolderPath(secondaryFolderPath: String) =
        dbHandler.setSecondaryFolderPath(secondaryFolderPath)

    override suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean) =
        dbHandler.setSecondaryUploadEnabled(secondaryCameraUpload)

    override suspend fun getSecondaryFolderPath(): String? =
        dbHandler.preferences?.localPathSecondaryFolder

    override suspend fun getRemoveGpsDefault(): Boolean =
        dbHandler.preferences?.removeGPS?.toBoolean() ?: true

    override suspend fun getUploadVideoQuality(): String? =
        dbHandler.preferences?.uploadVideoQuality

    override suspend fun getKeepFileNames(): Boolean =
        dbHandler.preferences?.keepFileNames.toBoolean()

    override suspend fun isFolderExternalSd(): Boolean =
        dbHandler.preferences?.cameraFolderExternalSDCard.toBoolean()

    override suspend fun getUriExternalSd(): String? = dbHandler.preferences?.uriExternalSDCard

    override suspend fun isSecondaryMediaFolderEnabled(): Boolean =
        dbHandler.preferences?.secondaryMediaFolderEnabled.toBoolean()

    override suspend fun isMediaFolderExternalSd(): Boolean = dbHandler.mediaFolderExternalSdCard

    override suspend fun getUriMediaFolderExternalSd(): String? = dbHandler.uriMediaExternalSdCard

    override suspend fun shouldClearSyncRecords(): Boolean = dbHandler.shouldClearCamsyncRecords()

    override suspend fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long =
        dbHandler.findMaxTimestamp(isSecondary, syncRecordType) ?: 0

    override suspend fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord> =
        dbHandler.findVideoSyncRecordsByState(syncStatusType)

    override suspend fun getChargingOnSizeString(): String =
        dbHandler.preferences?.chargingOnSize ?: DEFAULT_CONVENTION_QUEUE_SIZE.toString()

    override suspend fun convertOnCharging(): Boolean =
        doPreferencesExist() && dbHandler.preferences?.conversionOnCharging.toBoolean()

    override suspend fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    ) = dbHandler.updateSyncRecordStatusByLocalPath(syncStatusType, localPath, isSecondary)

    override suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo? =
        dbHandler.findNonContactByHandle(userHandle.toString())

    override suspend fun setNonContactEmail(userHandle: Long, email: String) {
        dbHandler.setNonContactEmail(email, userHandle.toString())
    }

    override suspend fun getContactByEmail(email: String?) =
        dbHandler.findContactByEmail(email)

    override suspend fun setUserHasLoggedIn() {
        dbHandler.setFirstTime(false)
    }

    override suspend fun setStorageAskAlways(isStorageAskAlways: Boolean) {
        dbHandler.setStorageAskAlways(isStorageAskAlways)
    }

    override suspend fun setStorageDownloadLocation(storageDownloadLocation: String) {
        dbHandler.setStorageDownloadLocation(storageDownloadLocation)
    }

    override fun setPasscodeLockEnabled(isPasscodeLockEnabled: Boolean) {
        dbHandler.isPasscodeLockEnabled = isPasscodeLockEnabled
    }

    override suspend fun setPasscodeLockCode(passcodeLockCode: String) {
        dbHandler.passcodeLockCode = passcodeLockCode
    }

    override suspend fun setShowCopyright(showCopyrights: Boolean) {
        dbHandler.setShowCopyright(showCopyrights)
    }

    override suspend fun setCamSyncLocalPath(path: String?) {
        path?.let { it ->
            dbHandler.setCamSyncLocalPath(it)
        }
    }

    override suspend fun setCameraFolderExternalSDCard(cameraFolderExternalSDCard: Boolean) {
        dbHandler.setCameraFolderExternalSDCard(cameraFolderExternalSDCard)
    }

    override suspend fun setCamSyncWifi(enableCellularSync: Boolean) {
        dbHandler.setCamSyncWifi(enableCellularSync)
    }

    override suspend fun setCamSyncFileUpload(fileUpload: Int) {
        dbHandler.setCamSyncFileUpload(fileUpload)
    }

    override suspend fun setCameraUploadVideoQuality(quality: Int) {
        dbHandler.setCameraUploadVideoQuality(quality)
    }

    override suspend fun setConversionOnCharging(onCharging: Boolean) {
        dbHandler.setConversionOnCharging(onCharging)
    }

    override suspend fun setChargingOnSize(size: Int) {
        dbHandler.setChargingOnSize(size)
    }

    override suspend fun setCamSyncEnabled(enable: Boolean) {
        dbHandler.setCamSyncEnabled(enable)
    }
}
