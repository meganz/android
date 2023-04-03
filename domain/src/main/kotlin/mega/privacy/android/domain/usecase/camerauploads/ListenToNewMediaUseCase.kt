package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that listens to new Photos and Videos captured by the Device
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class ListenToNewMediaUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() = cameraUploadRepository.listenToNewMedia()
}