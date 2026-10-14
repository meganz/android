package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * App-wide snapshot of every sync's run, shared by every surface so they agree on one run.
 *
 * @property isSeeded False until the first successful read of the sync list
 * @property syncs Every known sync, sorted by syncId
 * @property progress The current run's progress
 */
internal data class SyncRunSnapshot(
    val isSeeded: Boolean,
    val syncs: List<SyncWorkState>,
    val progress: SyncEpisodeProgress,
) {
    /**
     * True while not seeded, or while any sync has work, except a running sync that has an error,
     * because the SDK skips such a sync, so its stats are not transfer work.
     */
    val hasTransferWork: Boolean
        get() = !isSeeded || syncs.any { sync ->
            sync.hasWork && !(sync.runState == SyncRunState.Running && sync.hasError)
        }

    companion object {
        val NotSeeded = SyncRunSnapshot(
            isSeeded = false,
            syncs = emptyList(),
            progress = SyncEpisodeProgress.None,
        )
    }
}
