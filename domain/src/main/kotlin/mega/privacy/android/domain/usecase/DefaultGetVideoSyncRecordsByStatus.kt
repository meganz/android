package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
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
