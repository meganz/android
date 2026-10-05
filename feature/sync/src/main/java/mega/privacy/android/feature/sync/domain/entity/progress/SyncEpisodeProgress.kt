package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * Progress of the current run, shared by the widget and the screen.
 *
 * @property processedItems Files done in this run; never decreases within an episode
 * @property pendingItems Files the live syncs still have pending
 * @property interruptedItems Files left pending by syncs suspended during this run
 * @property fraction Share of the run done; null while indeterminate; never decreases within an episode
 */
internal data class SyncEpisodeProgress(
    val episodeId: Long,
    val processedItems: Int,
    val pendingItems: Int,
    val interruptedItems: Int,
    val fraction: Float?,
) {
    val totalItems: Int get() = processedItems + pendingItems

    companion object {
        val None = SyncEpisodeProgress(
            episodeId = 0,
            processedItems = 0,
            pendingItems = 0,
            interruptedItems = 0,
            fraction = null,
        )
    }
}
