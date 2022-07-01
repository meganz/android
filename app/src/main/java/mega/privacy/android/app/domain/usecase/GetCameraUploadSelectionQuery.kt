package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository.SyncTimeStamp

/**
 * Build the camera upload SQL selection query string
 */
interface GetCameraUploadSelectionQuery {

    /**
     * Invoke
     *
     * @return selection query or null
     */
    operator fun invoke(timestampType: SyncTimeStamp): String?
}
