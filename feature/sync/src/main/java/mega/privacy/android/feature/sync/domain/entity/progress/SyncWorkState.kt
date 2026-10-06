package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * What the progress feature knows about one sync or backup.
 * [stats] is set only while Running, and is null until the first sample after entering Running.
 * [runningSinceMillis] is when this process saw the sync enter Running; null if unknown.
 * [hasError] is true while the SDK reports an error for the sync, in any run state.
 */
internal data class SyncWorkState(
    val syncId: Long,
    val runState: SyncRunState,
    val isStorageOverQuota: Boolean,
    val stats: SyncStatsSample?,
    val runningSinceMillis: Long?,
    val hasError: Boolean = false,
) {
    /**
     * Pending or Loading, or Running and scanning or with pending items.
     * The SDK reports isSyncing on idle syncs too, so it is not work on its own.
     */
    val hasWork: Boolean
        get() = runState == SyncRunState.Pending ||
                runState == SyncRunState.Loading ||
                (runState == SyncRunState.Running && stats != null &&
                        (stats.isScanning || stats.pendingItems > 0))
}
