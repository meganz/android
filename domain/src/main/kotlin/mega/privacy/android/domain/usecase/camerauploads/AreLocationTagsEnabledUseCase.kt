package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that checks whether Location Tags are added or not, when uploading Photos
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class AreLocationTagsEnabledUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return true if Location Tags should be added when uploading Photos, and false if otherwise
     */
    suspend operator fun invoke(): Boolean =
        cameraUploadRepository.areLocationTagsEnabled() ?: false
}
