package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Get Camera Upload Backup ID Use Case
 *
 * @param cameraUploadRepository [CameraUploadRepository]
 * @return [Long]
 */
class GetCameraUploadBackupIDUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {
    /**
     * Invocation function
     */
    suspend operator fun invoke() = cameraUploadRepository.getCuBackUpId()
}
