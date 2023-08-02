package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default Implementation of [RestoreSecondaryTimestamps]
 */
class DefaultRestoreSecondaryTimestamps @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cameraUploadRepository: CameraUploadRepository,
) : RestoreSecondaryTimestamps {
    override suspend fun invoke() {
        val backedUpHandle = settingsRepository.getSecondaryHandle()
        val detectedSecondaryHandle = cameraUploadRepository.getSecondarySyncHandle()
        if (backedUpHandle != null && detectedSecondaryHandle != null && detectedSecondaryHandle == backedUpHandle) {
            // if the primary handle matches to previous deleted primary folder's handle, restore the time stamp
            val camSyncTimestamp = settingsRepository.getSecondaryFolderPhotoSyncTime()
            if (!camSyncTimestamp.isNullOrEmpty()) {
                runCatching {
                    cameraUploadRepository.setSyncTimeStamp(
                        camSyncTimestamp.toLong(),
                        SyncTimeStamp.SECONDARY_PHOTO
                    )
                }
            }
            val camVideoSyncTimestamp = settingsRepository.getSecondaryFolderVideoSyncTime()
            if (!camVideoSyncTimestamp.isNullOrEmpty()) {
                runCatching {
                    cameraUploadRepository.setSyncTimeStamp(
                        camVideoSyncTimestamp.toLong(),
                        SyncTimeStamp.SECONDARY_VIDEO
                    )
                }
            }
        } else {
            cameraUploadRepository.deleteAllSecondarySyncRecords()
        }
        // clear sync records after restoration
        settingsRepository.clearSecondaryCameraSyncRecords()
    }
}
