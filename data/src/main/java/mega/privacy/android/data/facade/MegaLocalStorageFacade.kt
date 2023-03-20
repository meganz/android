package mega.privacy.android.data.facade

import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.model.ChatSettings
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.user.UserCredentials
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_FAV_ASC
import nz.mega.sdk.MegaApiJava.ORDER_LABEL_ASC
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
internal class MegaLocalStorageFacade @Inject constructor(
    private val dbHandler: DatabaseHandler,
) : MegaLocalStorageGateway {

    override suspend fun getCamSyncHandle(): Long? =
        dbHandler.preferences?.camSyncHandle?.toLongOrNull()

    override suspend fun getMegaHandleSecondaryFolder(): Long? =
        dbHandler.preferences?.megaHandleSecondaryFolder?.toLongOrNull()

    override suspend fun setCamSyncHandle(primaryHandle: Long) {
        dbHandler.preferences?.camSyncHandle = primaryHandle.toString()
        setPrimaryFolderHandle(primaryHandle)
    }

    override suspend fun setCamSyncSecondaryHandle(secondaryHandle: Long) {
        dbHandler.preferences?.megaHandleSecondaryFolder = secondaryHandle.toString()
        setSecondaryFolderHandle(secondaryHandle)
    }

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

    /**
     * Since offline nodes cannot be ordered by labels and favorites, the offline order will be same as
     * cloud order except when cloud order is ORDER_LABEL_ASC or ORDER_FAV_ASC where it defaults to
     * ORDER_DEFAULT_ASC.
     */
    override suspend fun getOfflineSortOrder(): Int =
        when (val order = getCloudSortOrder()) {
            ORDER_LABEL_ASC -> ORDER_DEFAULT_ASC
            ORDER_FAV_ASC -> ORDER_DEFAULT_ASC
            else -> order
        }

    override suspend fun setOfflineSortOrder(order: Int) {
        dbHandler.setPreferredSortCloud(order.toString())
    }

    override suspend fun setCloudSortOrder(order: Int) {
        dbHandler.setPreferredSortCloud(order.toString())
    }

    override suspend fun setCameraSortOrder(order: Int) {
        dbHandler.setPreferredSortCameraUpload(order.toString())
    }

    override suspend fun setOthersSortOrder(order: Int) {
        dbHandler.setPreferredSortOthers(order.toString())
    }

    override suspend fun getUserCredentials(): UserCredentials? = dbHandler.credentials

    override suspend fun isSyncByWifi(): Boolean =
        dbHandler.preferences?.camSyncWifi?.toBoolean() ?: true

    override suspend fun getPendingSyncRecords(): List<SyncRecord> =
        dbHandler.findAllPendingSyncRecords()

    override suspend fun getCameraSyncFileUpload(): String? =
        dbHandler.preferences?.camSyncFileUpload

    override suspend fun setCameraSyncFileUpload(uploadOption: Int) =
        dbHandler.setCamSyncFileUpload(uploadOption)

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

    override suspend fun getPhotoTimeStamp() = dbHandler.preferences?.camSyncTimeStamp

    override suspend fun getSecondaryPhotoTimeStamp() = dbHandler.preferences?.secSyncTimeStamp

    override suspend fun getVideoTimeStamp() = dbHandler.preferences?.camVideoSyncTimeStamp

    override suspend fun getSecondaryVideoTimeStamp() = dbHandler.preferences?.secVideoSyncTimeStamp

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

    override suspend fun doesSyncEnabledExist() = dbHandler.preferences?.camSyncEnabled != null

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

    override suspend fun areLocationTagsEnabled(): Boolean =
        dbHandler.preferences?.removeGPS?.toBoolean()?.not() ?: false

    override suspend fun setLocationTagsEnabled(enable: Boolean) =
        dbHandler.setRemoveGPS(!enable)

    override suspend fun getUploadVideoQuality(): String =
        dbHandler.preferences?.uploadVideoQuality ?: VideoQuality.ORIGINAL.value.toString()

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

    override suspend fun isChargingRequiredForVideoCompression(): Boolean =
        dbHandler.preferences?.conversionOnCharging?.toBoolean() ?: true

    override suspend fun setChargingRequiredForVideoCompression(chargingRequired: Boolean) {
        dbHandler.setConversionOnCharging(chargingRequired)
    }

    override suspend fun getVideoCompressionSizeLimit(): Int =
        dbHandler.preferences?.chargingOnSize?.toInt() ?: DEFAULT_CONVENTION_QUEUE_SIZE

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

    override suspend fun getStorageAskAlways(): Boolean =
        dbHandler.preferences?.storageAskAlways?.toBoolean() ?: true

    override suspend fun setStorageAskAlways(isStorageAskAlways: Boolean) {
        dbHandler.setStorageAskAlways(isStorageAskAlways)
    }

    override suspend fun getStorageDownloadLocation(): String? =
        dbHandler.preferences?.storageDownloadLocation

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

    private fun setPrimaryFolderHandle(primaryHandle: Long) {
        dbHandler.setCamSyncHandle(primaryHandle)
    }

    private fun setSecondaryFolderHandle(secondaryHandle: Long) {
        dbHandler.setSecondaryFolderHandle(secondaryHandle)
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

    override suspend fun setUploadVideoQuality(quality: Int) {
        dbHandler.setCameraUploadVideoQuality(quality)
    }

    override suspend fun setUploadVideoSyncStatus(syncStatus: Int) {
        dbHandler.updateVideoState(syncStatus)
    }

    override suspend fun setChargingOnSize(size: Int) {
        dbHandler.setChargingOnSize(size)
    }

    override suspend fun setCamSyncEnabled(enable: Boolean) {
        dbHandler.setCamSyncEnabled(enable)
    }

    override suspend fun getAttributes(): MegaAttributes? =
        dbHandler.attributes

    override suspend fun saveShouldClearCamSyncRecords(clearCamSyncRecords: Boolean) {
        dbHandler.saveShouldClearCamsyncRecords(clearCamSyncRecords)
    }

    override suspend fun deleteAllPrimarySyncRecords() = dbHandler.deleteAllPrimarySyncRecords()

    override suspend fun deleteAllSecondarySyncRecords() = dbHandler.deleteAllSecondarySyncRecords()

    override suspend fun getChatFilesFolderHandle() = dbHandler.myChatFilesFolderHandle

    override suspend fun isOfflineInformationAvailable(nodeHandle: Long) =
        dbHandler.exists(nodeHandle)

    override suspend fun getOfflineInformation(nodeHandle: Long) =
        dbHandler.getOfflineInformation(nodeHandle)

    override suspend fun saveMyFirstName(firstName: String) =
        dbHandler.saveMyFirstName(firstName)

    override suspend fun saveMyLastName(lastName: String) =
        dbHandler.saveMyLastName(lastName)

    override suspend fun setLastPublicHandle(handle: Long) = dbHandler.setLastPublicHandle(handle)

    override suspend fun setLastPublicHandleTimeStamp() = dbHandler.setLastPublicHandleTimeStamp()

    override suspend fun setLastPublicHandleType(type: Int) {
        dbHandler.lastPublicHandleType = type
    }

    override suspend fun getChatSettings(): ChatSettings? = dbHandler.chatSettings

    override suspend fun setChatSettings(chatSettings: ChatSettings) {
        dbHandler.chatSettings = chatSettings
    }

    override suspend fun saveCredentials(userCredentials: UserCredentials) =
        dbHandler.saveCredentials(userCredentials)

    override suspend fun clearEphemeral() = dbHandler.clearEphemeral()

    override suspend fun clearCredentials() = dbHandler.clearCredentials()

    override suspend fun clearPreferences() = dbHandler.clearPreferences()

    override suspend fun setFirstTime(firstTime: Boolean) = dbHandler.setFirstTime(firstTime)

    override suspend fun getFirstTime(): Boolean? =
        dbHandler.preferences?.firstTime?.toBooleanStrictOrNull()

    override suspend fun clearOffline() = dbHandler.clearOffline()

    override suspend fun clearContacts() = dbHandler.clearContacts()

    override suspend fun clearNonContacts() = dbHandler.clearNonContacts()

    override suspend fun clearChatItems() = dbHandler.clearChatItems()

    override suspend fun clearCompletedTransfers() = dbHandler.clearCompletedTransfers()

    override suspend fun clearPendingMessages() = dbHandler.clearPendingMessage()

    override suspend fun clearAttributes() = dbHandler.clearAttributes()

    override suspend fun deleteAllSyncRecordsTypeAny() = dbHandler.deleteAllSyncRecordsTypeAny()

    override suspend fun clearChatSettings() = dbHandler.clearChatSettings()

    override suspend fun clearBackups() = dbHandler.clearBackups()

    override suspend fun clearMegaContacts() = dbHandler.clearMegaContacts()

    override suspend fun loadOfflineNodes(
        path: String,
        searchQuery: String?,
    ): List<OfflineInformation> = dbHandler.getOfflineInformationList(path, searchQuery)

    override suspend fun setContactNickName(nickName: String, handle: Long) =
        dbHandler.setContactNickname(nickname = nickName, handle = handle)

    companion object {
        private const val DEFAULT_CONVENTION_QUEUE_SIZE = 200
    }

}
