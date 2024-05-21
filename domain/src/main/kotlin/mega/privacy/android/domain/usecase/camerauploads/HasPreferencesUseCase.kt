package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use case to check if preferences exist
 */
class HasPreferencesUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {

    /**
     * Invoke
     *
     * @return do preferences exist
     */
    suspend operator fun invoke() = cameraUploadsRepository.doPreferencesExist()
}
