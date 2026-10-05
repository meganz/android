package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * The state of the current run that the progress figure is folded from.
 * The fraction never decreases within an episode; only a new [episodeId] resets it.
 */
internal data class SyncProgressEpisode(
    val episodeId: Long,
    val counters: Map<Long, Counter>,
    val retiredProcessed: Int,
    val interruptions: Map<Long, Interruption>,
    val fraction: Float?,
    val idleSinceMillis: Long?,
) {
    /**
     * Files a live sync has discovered and still has pending in this run.
     */
    data class Counter(val discovered: Int, val pending: Int, val awaitingScanEnd: Boolean)

    /**
     * Files a sync left pending when it was suspended during this run.
     */
    data class Interruption(val pendingItems: Int, val sinceMillis: Long)

    /**
     * Returns the progress figure the widget and the screen read.
     */
    fun toProgress(): SyncEpisodeProgress = SyncEpisodeProgress(
        episodeId = episodeId,
        processedItems = retiredProcessed + counters.values.sumOf { it.discovered - it.pending },
        pendingItems = counters.values.sumOf { it.pending },
        interruptedItems = interruptions.values.sumOf { it.pendingItems },
        fraction = fraction,
    )

    companion object {
        val Initial = SyncProgressEpisode(
            episodeId = 0,
            counters = emptyMap(),
            retiredProcessed = 0,
            interruptions = emptyMap(),
            fraction = null,
            idleSinceMillis = null,
        )
    }
}
