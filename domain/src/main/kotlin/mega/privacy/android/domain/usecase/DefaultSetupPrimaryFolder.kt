package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [SetupPrimaryFolder]
 */
class DefaultSetupPrimaryFolder @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val stopCameraUpload: StopCameraUpload,
    private val restartCameraUpload: RestartCameraUpload,
    private val resetPrimaryTimeline: ResetPrimaryTimeline,
    private val updateFolderIconBroadcast: UpdateFolderIconBroadcast,
    private val updateFolderDestinationBroadcast: UpdateFolderDestinationBroadcast,
) : SetupPrimaryFolder {
    override suspend fun invoke(primaryHandle: Long) {
        runCatching {
            cameraUploadRepository.setupPrimaryFolder(primaryHandle)
        }.onSuccess { newPrimaryHandle ->
            if (newPrimaryHandle != cameraUploadRepository.getInvalidHandle()) {
                resetPrimaryTimeline()
                cameraUploadRepository.setPrimarySyncHandle(newPrimaryHandle)
                updateFolderIconBroadcast(newPrimaryHandle, false)
                restartCameraUpload(shouldIgnoreAttributes = true)
                updateFolderDestinationBroadcast(newPrimaryHandle, false)
            }
        }.onFailure {
            stopCameraUpload()
        }
    }
}
