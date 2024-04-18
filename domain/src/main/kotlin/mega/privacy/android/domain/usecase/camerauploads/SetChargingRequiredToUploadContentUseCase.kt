package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to update the state in which the Device must be charged or not for the active Camera
 * Uploads to begin uploading content
 *
 * @property cameraUploadRepository Repository containing all Camera Uploads related functions
 */
class SetChargingRequiredToUploadContentUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param chargingRequired The new Device charging state
     */
    suspend operator fun invoke(chargingRequired: Boolean) =
        cameraUploadRepository.setChargingRequiredToUploadContent(chargingRequired)
}