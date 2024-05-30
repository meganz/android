package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that checks whether or not Media Uploads (Secondary Folder Uploads) are enabled
 *
 * @property cameraUploadsRepository Repository containing all Camera Uploads related operations
 */
class IsMediaUploadsEnabledUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @return true if Media Uploads is enabled, and false if otherwise. false is also returned
     * if the Setting saved in the DataStore is null
     */
    suspend operator fun invoke() = cameraUploadsRepository.isMediaUploadsEnabled() ?: false
}
