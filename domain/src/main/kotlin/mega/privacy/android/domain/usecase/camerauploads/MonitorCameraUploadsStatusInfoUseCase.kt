package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.flow.filter
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Monitor Camera Uploads Status Info UseCase
 */
class MonitorCameraUploadsStatusInfoUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadRepository,
) {

    /**
     * Invoke
     * @return flow of [CameraUploadsStatusInfo]
     */
    suspend operator fun invoke() =
        cameraUploadsRepository.monitorCameraUploadsStatusInfo()
            .filter { cameraUploadsRepository.isCameraUploadsEnabled() == true }
}
