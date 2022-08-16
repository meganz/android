package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord

/**
 * Get pending sync records
 *
 */
fun interface GetPendingSyncRecords {

    /**
     * Invoke
     *
     * @return pending sync records
     */
    suspend operator fun invoke(): List<SyncRecord>
}
