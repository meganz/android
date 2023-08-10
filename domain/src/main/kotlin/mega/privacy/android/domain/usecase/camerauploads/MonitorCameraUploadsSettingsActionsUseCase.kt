package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case for monitoring camera uploads setting actions
 */
class MonitorCameraUploadsSettingsActionsUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invoke
     * @return flow of [CameraUploadsSettingsAction]
     */
    operator fun invoke() = cameraUploadRepository.monitorCameraUploadsSettingsActions()
}
