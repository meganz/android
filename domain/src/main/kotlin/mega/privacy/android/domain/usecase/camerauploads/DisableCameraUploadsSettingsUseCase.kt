package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Disable the camera uploads settings
 *
 */
class DisableCameraUploadsSettingsUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Disable the camera uploads settings
     */
    suspend operator fun invoke() {
        cameraUploadRepository.setCameraUploadsEnabled(false)
        cameraUploadRepository.setSecondaryEnabled(false)
    }
}
