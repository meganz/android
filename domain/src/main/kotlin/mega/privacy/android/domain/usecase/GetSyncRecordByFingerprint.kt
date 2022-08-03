package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord

/**
 * Get sync record by fingerprint
 *
 */
fun interface GetSyncRecordByFingerprint {

    /**
     * Invoke
     *
     * @return sync record if found
     */
    operator fun invoke(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopyOnly: Boolean,
    ): SyncRecord?
}
