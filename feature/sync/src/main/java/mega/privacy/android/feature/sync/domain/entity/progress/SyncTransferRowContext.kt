package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * Account-wide inputs of the row rules besides the row itself.
 * [areTransfersPaused] is the app-wide transfer pause; the quota flags are the account blockers.
 */
internal data class SyncTransferRowContext(
    val areTransfersPaused: Boolean,
    val isStorageOverQuota: Boolean,
    val isTransferOverQuota: Boolean,
)
