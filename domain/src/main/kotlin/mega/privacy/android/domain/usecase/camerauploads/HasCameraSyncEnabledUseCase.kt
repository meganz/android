package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Checks if camera uploads setting enabled exists.
 */
class HasCameraSyncEnabledUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = cameraUploadRepository.doesSyncEnabledExist()
}