package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Broadcast camera uploads settings action
 */
class BroadcastCameraUploadsSettingsActionUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invoke
     * @param action [CameraUploadsSettingsAction]
     */
    suspend operator fun invoke(action: CameraUploadsSettingsAction) =
        cameraUploadsRepository.broadCastCameraUploadSettingsActions(action)
}
