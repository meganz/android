package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Checks if camera uploads setting enabled exists.
 */
class HasCameraSyncEnabledUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = cameraUploadsRepository.doesSyncEnabledExist()
}