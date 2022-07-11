package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Delete camera upload sync record by local path
 *
 */
class DefaultDeleteSyncRecordByLocalPath @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : DeleteSyncRecordByLocalPath {

    override fun invoke(localPath: String, isSecondary: Boolean) =
        cameraUploadRepository.deleteSyncRecordByLocalPath(localPath, isSecondary)
}
