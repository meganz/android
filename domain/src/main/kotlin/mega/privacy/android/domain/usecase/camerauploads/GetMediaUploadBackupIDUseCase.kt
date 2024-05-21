package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Get Media Upload Backup ID Use Case
 *
 * @param cameraUploadsRepository [CameraUploadsRepository]
 * @return [Long]
 */
class GetMediaUploadBackupIDUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {
    /**
     * Invocation function
     */
    suspend operator fun invoke() = cameraUploadsRepository.getMuBackUpId()
}
