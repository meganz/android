package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [CameraUploadRepository]
 *
 * @property localStorageGateway MegaLocalStorageGateway
 * @property ioDispatcher CoroutineDispatcher
 */
class DefaultCameraUploadRepository @Inject constructor(
    private val localStorageGateway: MegaLocalStorageGateway,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CameraUploadRepository {

    override fun getInvalidHandle(): Long {
        return megaApiGateway.getInvalidHandle()
    }


    override suspend fun getPrimarySyncHandle(): Long? = withContext(ioDispatcher) {
        localStorageGateway.getCamSyncHandle()
    }

    override suspend fun getSecondarySyncHandle(): Long? = withContext(ioDispatcher) {
        localStorageGateway.getMegaHandleSecondaryFolder()
    }

    override suspend fun setPrimarySyncHandle(primaryHandle: Long) = withContext(ioDispatcher) {
        localStorageGateway.setCamSyncHandle(primaryHandle)
    }

    override suspend fun setSecondarySyncHandle(secondaryHandle: Long) = withContext(ioDispatcher) {
        localStorageGateway.setMegaHandleSecondaryFolder(secondaryHandle)
    }

    override suspend fun isSyncByWifi() = withContext(ioDispatcher) {
        localStorageGateway.isSyncByWifi()
    }

    override suspend fun isSyncByWifiDefault() = withContext(ioDispatcher) {
        localStorageGateway.isSyncByWifiDefault()
    }

    override suspend fun getPendingSyncRecords(): List<SyncRecord> = withContext(ioDispatcher) {
        localStorageGateway.getPendingSyncRecords()
    }

    override suspend fun setPhotosSyncFileUpload() = withContext(ioDispatcher) {
        localStorageGateway.setPhotosSyncUpload()
    }

    override suspend fun getSyncFileUpload(): String? = withContext(ioDispatcher) {
        localStorageGateway.getCameraSyncFileUpload()
    }

    override suspend fun getVideoQuality(): String = withContext(ioDispatcher) {
        localStorageGateway.getVideoQuality()
    }

    override suspend fun deleteAllSyncRecords(syncRecordType: Int) = withContext(ioDispatcher) {
        localStorageGateway.deleteAllSyncRecords(syncRecordType)
    }

    override suspend fun deleteSyncRecord(path: String?, isSecondary: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.deleteSyncRecordByPath(path, isSecondary)
        }

    override suspend fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.deleteSyncRecordByLocalPath(localPath, isSecondary)
        }

    override suspend fun deleteSyncRecordByFingerprint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    ) =
        withContext(ioDispatcher) {
            localStorageGateway.deleteSyncRecordByFingerPrint(originalPrint,
                newPrint,
                isSecondary)
        }

    override suspend fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord? =
        withContext(ioDispatcher) {
            localStorageGateway.getSyncRecordByFingerprint(fingerprint, isSecondary, isCopy)
        }

    override suspend fun getSyncRecordByNewPath(path: String): SyncRecord? =
        withContext(ioDispatcher) {
            localStorageGateway.getSyncRecordByNewPath(path)
        }

    override suspend fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord? =
        withContext(ioDispatcher) {
            localStorageGateway.getSyncRecordByLocalPath(path, isSecondary)
        }

    override suspend fun shouldClearSyncRecords(clearSyncRecords: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.shouldClearSyncRecords(clearSyncRecords)
        }

    override suspend fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doesFileNameExist(fileName, isSecondary, type)
    }

    override suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doesLocalPathExist(fileName, isSecondary, type)
    }

    override suspend fun saveSyncRecord(record: SyncRecord) = withContext(ioDispatcher) {
        localStorageGateway.saveSyncRecord(record)
    }

    override suspend fun getSyncTimeStamp(type: SyncTimeStamp): Long {
        return withContext(ioDispatcher) {
            when (type) {
                SyncTimeStamp.PRIMARY_PHOTO -> localStorageGateway.getPhotoTimeStamp()
                SyncTimeStamp.PRIMARY_VIDEO -> localStorageGateway.getVideoTimeStamp()
                SyncTimeStamp.SECONDARY_PHOTO -> localStorageGateway.getSecondaryPhotoTimeStamp()
                SyncTimeStamp.SECONDARY_VIDEO -> localStorageGateway.getSecondaryVideoTimeStamp()
            }
        }
    }

    override suspend fun setSyncTimeStamp(timeStamp: Long, type: SyncTimeStamp) {
        withContext(ioDispatcher) {
            when (type) {
                SyncTimeStamp.PRIMARY_PHOTO -> localStorageGateway.setPhotoTimeStamp(timeStamp)
                SyncTimeStamp.PRIMARY_VIDEO -> localStorageGateway.setVideoTimeStamp(timeStamp)
                SyncTimeStamp.SECONDARY_PHOTO -> localStorageGateway.setSecondaryPhotoTimeStamp(
                    timeStamp)
                SyncTimeStamp.SECONDARY_VIDEO -> localStorageGateway.setSecondaryVideoTimeStamp(
                    timeStamp)
            }
        }
    }

    override suspend fun doCredentialsExist(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doCredentialsExist()
    }

    override suspend fun doPreferencesExist(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doPreferencesExist()
    }

    override suspend fun isSyncEnabled(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.isSyncEnabled()
    }

    override suspend fun getSyncLocalPath(): String? = withContext(ioDispatcher) {
        localStorageGateway.getSyncLocalPath()
    }

    override suspend fun setSyncLocalPath(localPath: String) = withContext(ioDispatcher) {
        localStorageGateway.setSyncLocalPath(localPath)
    }

    override suspend fun setSecondaryFolderPath(secondaryFolderPath: String) =
        withContext(ioDispatcher) {
            localStorageGateway.setSecondaryFolderPath(secondaryFolderPath)
        }

    override suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.setSecondaryEnabled(secondaryCameraUpload)
        }

    override suspend fun getSecondaryFolderPath(): String? = withContext(ioDispatcher) {
        localStorageGateway.getSecondaryFolderPath()
    }

    override suspend fun getRemoveGpsDefault(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.getRemoveGpsDefault()
    }

    override suspend fun getUploadVideoQuality(): String? = withContext(ioDispatcher) {
        localStorageGateway.getUploadVideoQuality()
    }

    override suspend fun getKeepFileNames(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.getKeepFileNames()
    }

    override suspend fun isFolderExternalSd(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.isFolderExternalSd()
    }

    override suspend fun getUriExternalSd(): String? = withContext(ioDispatcher) {
        localStorageGateway.getUriExternalSd()
    }

    override suspend fun isSecondaryMediaFolderEnabled() = withContext(ioDispatcher) {
        localStorageGateway.isSecondaryMediaFolderEnabled()
    }

    override suspend fun isMediaFolderExternalSd() = withContext(ioDispatcher) {
        localStorageGateway.isMediaFolderExternalSd()
    }

    override suspend fun getUriMediaFolderExternalSd(): String? = withContext(ioDispatcher) {
        localStorageGateway.getUriMediaFolderExternalSd()
    }

    override suspend fun shouldClearSyncRecords() = withContext(ioDispatcher) {
        localStorageGateway.shouldClearSyncRecords()
    }

    override suspend fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long =
        withContext(ioDispatcher) {
            localStorageGateway.getMaxTimestamp(isSecondary, syncRecordType)
        }

    override suspend fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord> =
        withContext(ioDispatcher) {
            localStorageGateway.getVideoSyncRecordsByStatus(syncStatusType)
        }

    override suspend fun getChargingOnSizeString(): String = withContext(ioDispatcher) {
        localStorageGateway.getChargingOnSizeString()
    }

    override suspend fun getChargingOnSize() = withContext(ioDispatcher) {
        localStorageGateway.getChargingOnSizeString().toInt()
    }

    override suspend fun convertOnCharging() = withContext(ioDispatcher) {
        localStorageGateway.convertOnCharging()
    }

    override suspend fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    ) = withContext(ioDispatcher) {
        localStorageGateway.updateSyncRecordStatusByLocalPath(syncStatusType,
            localPath,
            isSecondary)
    }

    override suspend fun backupTimestampsAndFolderHandle() = withContext(ioDispatcher) {
        val primaryHandle = localStorageGateway.getCamSyncHandle() ?: getInvalidHandle()
        val secondaryHandle =
            localStorageGateway.getMegaHandleSecondaryFolder() ?: getInvalidHandle()
        localStorageGateway.backupTimestampsAndFolderHandle(primaryHandle, secondaryHandle)
    }

    override suspend fun setCamSyncTimeStamp(value: Int) = withContext(ioDispatcher) {
        localStorageGateway.setCamSyncTimeStamp(value)
    }

    override suspend fun setCamVideoSyncTimeStamp(value: Int) = withContext(ioDispatcher) {
        localStorageGateway.setCamVideoSyncTimeStamp(value)
    }

    override suspend fun setSecSyncTimeStamp(value: Int) = withContext(ioDispatcher) {
        localStorageGateway.setSecSyncTimeStamp(value)
    }

    override suspend fun setSecVideoSyncTimeStamp(value: Int) = withContext(ioDispatcher) {
        localStorageGateway.setSecVideoSyncTimeStamp(value)
    }

    override suspend fun saveShouldClearCamSyncRecords(clearCamSyncRecords: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.saveShouldClearCamSyncRecords(clearCamSyncRecords)
        }
}
