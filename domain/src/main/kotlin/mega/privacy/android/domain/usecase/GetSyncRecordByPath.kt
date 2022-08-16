package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord

/**
 * Get sync record by path
 *
 */
interface GetSyncRecordByPath {

    /**
     * Invoke
     *
     * @return sync record if found
     */
    suspend operator fun invoke(path: String, isSecondary: Boolean): SyncRecord?
}
