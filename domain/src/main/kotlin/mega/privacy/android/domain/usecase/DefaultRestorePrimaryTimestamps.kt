package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default Implementation of [RestorePrimaryTimestamps]
 */
class DefaultRestorePrimaryTimestamps @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cameraUploadRepository: CameraUploadRepository,
) : RestorePrimaryTimestamps {
    override suspend fun invoke() {
        val backedUpHandle = settingsRepository.getPrimaryHandle()
        val detectedPrimaryHandle = cameraUploadRepository.getPrimarySyncHandle()
        if (backedUpHandle != null && detectedPrimaryHandle != null && detectedPrimaryHandle == backedUpHandle) {
            // if the primary handle matches to previous deleted primary folder's handle, restore the time stamp
            val camSyncTimestamp = settingsRepository.getPrimaryFolderPhotoSyncTime()
            if (!camSyncTimestamp.isNullOrEmpty()) {
                runCatching {
                    cameraUploadRepository.setSyncTimeStamp(camSyncTimestamp.toLong(),
                        SyncTimeStamp.PRIMARY_PHOTO)
                }
            }
            val camVideoSyncTimestamp = settingsRepository.getPrimaryFolderVideoSyncTime()
            if (!camVideoSyncTimestamp.isNullOrEmpty()) {
                runCatching {
                    cameraUploadRepository.setSyncTimeStamp(camVideoSyncTimestamp.toLong(),
                        SyncTimeStamp.PRIMARY_VIDEO)
                }
            }
        } else {
            cameraUploadRepository.deleteAllPrimarySyncRecords()
        }
        // clear sync records after restoration
        settingsRepository.clearPrimaryCameraSyncRecords()
    }
}