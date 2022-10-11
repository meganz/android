package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.SyncRecord
import java.util.Queue

/**
 * Use case to prepare sync record lists for camera upload
 */
interface GetPendingUploadList {

    /**
     * Invoke
     *
     * @return list of sync records
     */
    suspend operator fun invoke(
        mediaList: Queue<CameraUploadMedia>,
        isSecondary: Boolean,
        isVideo: Boolean,
    ): List<SyncRecord>
}
