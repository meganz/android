package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
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
