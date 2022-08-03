package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncStatus

/**
 * Get video sync records by status
 *
 */
fun interface GetVideoSyncRecordsByStatus {

    /**
     * Invoke
     *
     * @return video sync records
     */
    operator fun invoke(syncStatus: SyncStatus): List<SyncRecord>
}
