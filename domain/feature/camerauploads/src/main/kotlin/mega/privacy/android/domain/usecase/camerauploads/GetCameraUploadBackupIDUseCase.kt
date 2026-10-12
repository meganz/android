package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Get Camera Upload Backup ID Use Case
 *
 * @param cameraUploadsRepository [CameraUploadsRepository]
 * @return [Long]
 */
class GetCameraUploadBackupIDUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {
    /**
     * Invocation function
     */
    suspend operator fun invoke() = cameraUploadsRepository.getCuBackUpId()
}
