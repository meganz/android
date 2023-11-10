package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Build the camera upload SQL selection query string
 */
class GetCameraUploadSelectionQueryUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
) {

    /**
     * invoke
     * @param timestampType [SyncTimeStamp]
     */
    suspend operator fun invoke(timestampType: SyncTimeStamp): String? {
        val currentTimeStamp =
            cameraUploadRepository.getSyncTimeStamp(timestampType) ?: 0L

        // Do not create selection query if secondary preferences are not enabled or set (null)
        if (timestampType == SyncTimeStamp.SECONDARY_PHOTO || timestampType == SyncTimeStamp.SECONDARY_VIDEO) {
            if (cameraUploadRepository.isSecondaryMediaFolderEnabled() == false) {
                return null
            }
        }

        val localPath = when (timestampType) {
            SyncTimeStamp.PRIMARY_PHOTO, SyncTimeStamp.PRIMARY_VIDEO -> getPrimaryFolderPathUseCase()
            SyncTimeStamp.SECONDARY_PHOTO, SyncTimeStamp.SECONDARY_VIDEO -> getSecondaryFolderPathUseCase()
        }

        return cameraUploadRepository.getSelectionQuery(currentTimeStamp, localPath)
    }
}
