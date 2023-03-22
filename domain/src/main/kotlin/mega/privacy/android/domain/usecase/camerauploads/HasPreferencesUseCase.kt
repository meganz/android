package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use case to check if preferences exist
 */
class HasPreferencesUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {

    /**
     * Invoke
     *
     * @return do preferences exist
     */
    suspend operator fun invoke() = cameraUploadRepository.doPreferencesExist()
}
