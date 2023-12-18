package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import javax.inject.Inject

/**
 * Use Case to stop camera upload
 */
class StopCameraUploadsUseCase @Inject constructor(
    private val disableCameraUploadsUseCase: DisableCameraUploadsUseCase,
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * invoke
     * @param restartMode [CameraUploadsRestartMode]
     */
    suspend operator fun invoke(restartMode: CameraUploadsRestartMode) {
        if (cameraUploadRepository.isCameraUploadsEnabled() == true) {
            when (restartMode) {
                CameraUploadsRestartMode.RestartImmediately -> {
                    cameraUploadRepository.stopCameraUploads()
                    cameraUploadRepository.startCameraUploads()
                }

                CameraUploadsRestartMode.Reschedule -> {
                    cameraUploadRepository.stopCameraUploads()
                    cameraUploadRepository.scheduleCameraUploads()
                }

                CameraUploadsRestartMode.StopAndDisable -> {
                    cameraUploadRepository.stopCameraUploadsAndBackupHeartbeat()
                    disableCameraUploadsUseCase()
                }

                CameraUploadsRestartMode.Stop -> {
                    cameraUploadRepository.stopCameraUploads()
                }
            }
        }
    }
}
