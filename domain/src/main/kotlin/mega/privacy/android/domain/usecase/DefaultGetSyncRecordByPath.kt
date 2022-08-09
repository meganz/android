package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import javax.inject.Inject

/**
 * Get sync record by path
 *
 */
class DefaultGetSyncRecordByPath @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetSyncRecordByPath {

    override fun invoke(path: String, isSecondary: Boolean): SyncRecord? {
        return cameraUploadRepository.getSyncRecordByNewPath(path)
            ?: cameraUploadRepository.getSyncRecordByLocalPath(path, isSecondary)
    }
}
