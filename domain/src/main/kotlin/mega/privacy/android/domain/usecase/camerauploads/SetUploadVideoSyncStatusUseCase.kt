package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to update the Video Sync Status for Camera Uploads
 */
class SetUploadVideoSyncStatusUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param syncStatus The new [SyncStatus]
     */
    suspend operator fun invoke(syncStatus: SyncStatus) =
        cameraUploadRepository.setUploadVideoSyncStatus(syncStatus)
}