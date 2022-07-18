package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncStatus

/**
 * Get video sync records by status
 *
 */
interface GetVideoSyncRecordsByStatus {

    /**
     * Invoke
     *
     * @return video sync records
     */
    operator fun invoke(syncStatus: SyncStatus): List<SyncRecord>
}
