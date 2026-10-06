package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * App-wide view of sync activity, shared by every surface so they agree on one run.
 *
 * @property isSeeded False until the first successful read of the sync list
 * @property syncs Every known sync, sorted by syncId
 * @property progress The current run's progress
 */
internal data class SyncActivity(
    val isSeeded: Boolean,
    val syncs: List<SyncWorkState>,
    val progress: SyncEpisodeProgress,
) {
    companion object {
        val NotSeeded = SyncActivity(
            isSeeded = false,
            syncs = emptyList(),
            progress = SyncEpisodeProgress.None,
        )
    }
}
