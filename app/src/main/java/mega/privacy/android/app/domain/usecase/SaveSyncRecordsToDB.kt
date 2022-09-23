package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import nz.mega.sdk.MegaNode

/**
 * Helper use case to save sync records to database
 * Can be replaced by a worker after the camera upload service is removed
 *
 */
interface SaveSyncRecordsToDB {

    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(
        list: List<SyncRecord>,
        primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        rootPath: String?,
    )
}
