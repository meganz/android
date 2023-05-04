package mega.privacy.android.app.domain.usecase

import android.provider.MediaStore
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [GetCameraUploadSelectionQuery]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getCameraUploadLocalPathSecondary [GetCameraUploadLocalPathSecondary]
 * @property getPrimaryFolderPathUseCase [GetPrimaryFolderPathUseCase],
 */
class DefaultGetCameraUploadSelectionQuery @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getCameraUploadLocalPathSecondary: GetCameraUploadLocalPathSecondary,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
) : GetCameraUploadSelectionQuery {

    override suspend fun invoke(timestampType: SyncTimeStamp): String? {
        val currentTimeStamp =
            cameraUploadRepository.getSyncTimeStamp(timestampType) ?: 0L

        // Do not create selection query if secondary preferences are not enabled or set (null)
        if (timestampType == SyncTimeStamp.SECONDARY_PHOTO || timestampType == SyncTimeStamp.SECONDARY_VIDEO) {
            if (currentTimeStamp == 0L || !cameraUploadRepository.isSecondaryMediaFolderEnabled()) {
                return null
            }
        }

        Timber.d("%s timestamp is: %s", timestampType.toString(), currentTimeStamp)
        val localPath = when (timestampType) {
            SyncTimeStamp.PRIMARY_PHOTO, SyncTimeStamp.PRIMARY_VIDEO -> getPrimaryFolderPathUseCase()
            SyncTimeStamp.SECONDARY_PHOTO, SyncTimeStamp.SECONDARY_VIDEO -> getCameraUploadLocalPathSecondary()
        }

        @Suppress("DEPRECATION")
        return """((${MediaStore.MediaColumns.DATE_MODIFIED}*1000) > $currentTimeStamp OR (${MediaStore.MediaColumns.DATE_ADDED}*1000) > $currentTimeStamp) AND ${MediaStore.MediaColumns.DATA} LIKE '${localPath}%'"""
    }
}
