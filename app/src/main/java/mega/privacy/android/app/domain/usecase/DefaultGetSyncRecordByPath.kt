package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
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
