package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [GetUploadFolderHandle]
 *
 * @property cameraUploadRepository CameraUploadRepository
 */
class DefaultGetUploadFolderHandle @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) :
    GetUploadFolderHandle {
    override suspend fun invoke(isPrimary: Boolean): Long {
        val handle =
            if (isPrimary) cameraUploadRepository.getPrimarySyncHandle() else cameraUploadRepository.getSecondarySyncHandle()
        return handle ?: cameraUploadRepository.getInvalidHandle()
    }
}
