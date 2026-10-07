package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * The episode status every sync progress surface shares.
 *
 * @property isSeeded False until the sync list has been read; check it before [phase]
 * @property phase The current phase
 * @property progress The current run's progress
 */
internal data class SyncProgressStatus(
    val isSeeded: Boolean,
    val phase: SyncProgressPhase,
    val progress: SyncEpisodeProgress,
) {
    /**
     * Files still to do: the interrupted items while paused, else the pending items.
     */
    val filesLeft: Int
        get() = if (phase is SyncProgressPhase.Paused) {
            progress.interruptedItems
        } else {
            progress.pendingItems
        }
}
