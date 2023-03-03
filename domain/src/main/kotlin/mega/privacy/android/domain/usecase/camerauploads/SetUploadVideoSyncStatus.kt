package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.SyncStatus

/**
 * Use Case to update the Video Sync Status for Camera Uploads
 */
fun interface SetUploadVideoSyncStatus {

    /**
     * Invocation function
     *
     * @param syncStatus The new [SyncStatus]
     */
    suspend operator fun invoke(syncStatus: SyncStatus)
}