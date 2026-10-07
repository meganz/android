package mega.privacy.android.feature.sync.domain.entity.progress

/** Conditions that can stop sync transfers from moving, independent of the syncs' own state. */
internal data class SyncProgressBlockers(
    val isOffline: Boolean,
    val isStorageOverQuota: Boolean,
    val isTransferOverQuota: Boolean,
    val areTransfersPaused: Boolean,
    val stalledIssueCount: Int,
)
