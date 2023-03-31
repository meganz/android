package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Camera Uploads Primary Folder handle
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class GetPrimarySyncHandleUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return The Primary Folder Sync Handle
     */
    suspend operator fun invoke(): Long = with(cameraUploadRepository) {
        getPrimarySyncHandle() ?: getInvalidHandle()
    }
}
