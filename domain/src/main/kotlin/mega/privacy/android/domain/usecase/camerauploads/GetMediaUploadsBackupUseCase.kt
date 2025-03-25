package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Get Media Uploads Backup Use Case
 *
 * @param cameraUploadsRepository [CameraUploadsRepository]
 * @return The Camera Uploads backup or null if does not exist
 */
class GetMediaUploadsBackupUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {
    /**
     * Invocation function
     */
    suspend operator fun invoke(): Backup? = cameraUploadsRepository.getMuBackUp()
}