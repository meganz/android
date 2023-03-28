package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to stop camera upload and heartbeat workers
 */
class StopCameraUploadAndHeartbeatUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() = cameraUploadRepository.stopCameraUploadSyncHeartbeatWorkers()
}
