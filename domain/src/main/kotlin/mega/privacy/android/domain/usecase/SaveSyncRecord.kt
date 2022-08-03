package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord

/**
 * Save sync record
 *
 */
fun interface SaveSyncRecord {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(syncRecord: SyncRecord)
}
