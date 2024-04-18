package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that checks whether or not the Device must be charged for the active Camera Uploads to
 * start uploading content
 *
 * @property cameraUploadRepository Repository containing all Camera Uploads related functions
 */
class IsChargingRequiredToUploadContentUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return true if the Device must be charged for the active Camera Uploads to upload content.
     * It defaults to false if the value could not be retrieved
     */
    suspend operator fun invoke() =
        cameraUploadRepository.isChargingRequiredToUploadContent() ?: false
}