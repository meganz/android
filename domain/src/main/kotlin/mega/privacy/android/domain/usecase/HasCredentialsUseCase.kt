package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use case to check if credentials exist
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class HasCredentialsUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return do credentials exist
     */
    suspend operator fun invoke(): Boolean = cameraUploadRepository.hasCredentials()
}
