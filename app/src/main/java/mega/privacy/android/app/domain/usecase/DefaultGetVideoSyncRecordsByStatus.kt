package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncStatus
import javax.inject.Inject

/**
 * Get video sync records by status
 *
 */
class DefaultGetVideoSyncRecordsByStatus @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetVideoSyncRecordsByStatus {

    override fun invoke(syncStatus: SyncStatus) =
        cameraUploadRepository.getVideoSyncRecordsByStatus(syncStatus.value)
}
