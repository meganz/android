package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Broadcast camera uploads settings action
 */
class BroadcastCameraUploadsSettingsActionUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invoke
     * @param action [CameraUploadsSettingsAction]
     */
    suspend operator fun invoke(action: CameraUploadsSettingsAction) =
        cameraUploadRepository.broadCastCameraUploadSettingsActions(action)
}
