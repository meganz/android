package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case for monitoring camera uploads setting actions
 */
class MonitorCameraUploadsSettingsActionsUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {
    /**
     * Invoke
     * @return flow of [CameraUploadsSettingsAction]
     */
    operator fun invoke() = cameraUploadsRepository.monitorCameraUploadsSettingsActions()
}
