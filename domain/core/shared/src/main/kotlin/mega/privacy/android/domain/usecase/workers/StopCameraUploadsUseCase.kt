package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import javax.inject.Inject

/**
 * Use Case to stop camera upload
 */
class StopCameraUploadsUseCase @Inject constructor(
    private val disableCameraUploadsUseCase: DisableCameraUploadsUseCase,
    private val cameraUploadsRepository: CameraUploadsRepository,
) {
    /**
     * invoke
     * @param restartMode [CameraUploadsRestartMode]
     */
    suspend operator fun invoke(restartMode: CameraUploadsRestartMode) {
        if (cameraUploadsRepository.isCameraUploadsEnabled() == true) {
            when (restartMode) {
                CameraUploadsRestartMode.RestartImmediately -> {
                    cameraUploadsRepository.stopCameraUploads()
                    cameraUploadsRepository.startCameraUploads()
                }

                CameraUploadsRestartMode.Reschedule -> {
                    cameraUploadsRepository.stopCameraUploads()
                    cameraUploadsRepository.scheduleCameraUploads()
                }

                CameraUploadsRestartMode.StopAndDisable -> {
                    cameraUploadsRepository.stopCameraUploadsAndBackupHeartbeat()
                    disableCameraUploadsUseCase()
                }

                CameraUploadsRestartMode.Stop -> {
                    cameraUploadsRepository.stopCameraUploads()
                }
            }
        }
    }
}
