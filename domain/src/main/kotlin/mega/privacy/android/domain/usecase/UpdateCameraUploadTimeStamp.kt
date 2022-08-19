package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncTimeStamp


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
