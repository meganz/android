package mega.privacy.android.app.domain.usecase

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
    operator fun invoke(path: String, isSecondary: Boolean): SyncRecord?
}
