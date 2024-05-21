package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Camera Uploads Primary Folder handle
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class GetPrimarySyncHandleUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @return The Primary Folder Sync Handle
     */
    suspend operator fun invoke(): Long = with(cameraUploadsRepository) {
        getPrimarySyncHandle() ?: getInvalidHandle()
    }
}
