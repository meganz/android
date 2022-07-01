package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository.SyncTimeStamp

/**
 * Update camera upload primary/secondary photos/videos time stamp
 */
interface UpdateCameraUploadTimeStamp {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(timestamp: Long? = null, timestampType: SyncTimeStamp)
}
