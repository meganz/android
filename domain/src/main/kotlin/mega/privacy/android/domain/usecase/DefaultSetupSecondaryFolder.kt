package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [SetupSecondaryFolder]
 */
class DefaultSetupSecondaryFolder @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val startCameraUpload: StartCameraUpload,
    private val stopCameraUpload: StopCameraUpload,
    private val resetSecondaryTimeline: ResetSecondaryTimeline,
    private val updateFolderIconBroadcast: UpdateFolderIconBroadcast,
    private val updateFolderDestinationBroadcast: UpdateFolderDestinationBroadcast,
) : SetupSecondaryFolder {
    override suspend fun invoke(secondaryHandle: Long) {
        runCatching {
            cameraUploadRepository.setupSecondaryFolder(secondaryHandle)
        }.onSuccess { newSecondaryHandle ->
            if (newSecondaryHandle != cameraUploadRepository.getInvalidHandle()) {
                resetSecondaryTimeline()
                cameraUploadRepository.setSecondarySyncHandle(newSecondaryHandle)
                updateFolderIconBroadcast(newSecondaryHandle, true)
                stopCameraUpload()
                startCameraUpload(true)
                updateFolderDestinationBroadcast(newSecondaryHandle, true)
            }
        }.onFailure {
            stopCameraUpload()
        }
    }
}
