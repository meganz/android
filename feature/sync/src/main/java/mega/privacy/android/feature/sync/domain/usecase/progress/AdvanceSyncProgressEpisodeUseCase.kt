package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Companion.Initial
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Counter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Interruption
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Returns the episode after applying the current state of every sync at `nowMillis`.
 * The fraction never decreases within an episode; only a new episode id resets it.
 * Time is passed in so the use case reads no clock.
 */
internal class AdvanceSyncProgressEpisodeUseCase @Inject constructor() {

    operator fun invoke(
        episode: SyncProgressEpisode,
        syncs: List<SyncWorkState>,
        nowMillis: Long,
    ): SyncProgressEpisode {
        val hasWork = syncs.any { it.hasWork }
        val idleSinceMillis = episode.idleSinceMillis
        val base = if (hasWork && idleSinceMillis != null &&
            nowMillis - idleSinceMillis >= EPISODE_IDLE_GRACE.inWholeMilliseconds
        ) {
            Initial.copy(episodeId = episode.episodeId + 1)
        } else {
            episode
        }
        val byId = syncs.associateBy { it.syncId }
        var retired = base.retiredProcessed
        val interruptions = base.interruptions.toMutableMap()
        base.counters.forEach { (id, counter) ->
            val sync = byId[id]
            if (sync?.isLive != true) {
                retired += counter.processed
                if (sync?.runState == SyncRunState.Suspended && counter.pending > 0) {
                    interruptions[id] = Interruption(counter.pending, nowMillis)
                }
            }
        }
        interruptions.entries.removeAll { (id, interruption) ->
            val runState = byId[id]?.runState
            runState == null || runState == SyncRunState.Running ||
                    runState == SyncRunState.Disabled ||
                    nowMillis - interruption.sinceMillis >= INTERRUPTION_HOLD.inWholeMilliseconds
        }
        val counters = syncs.filter { it.isLive }.associate { sync ->
            sync.syncId to nextCounter(base.counters[sync.syncId], requireNotNull(sync.stats))
        }
        val idleSince = if (hasWork || interruptions.isNotEmpty()) {
            null
        } else {
            base.idleSinceMillis ?: nowMillis
        }
        return SyncProgressEpisode(
            episodeId = base.episodeId,
            counters = counters,
            retiredProcessed = retired,
            interruptions = interruptions,
            fraction = nextFraction(base.fraction, counters, retired, interruptions),
            idleSinceMillis = idleSince,
        )
    }

    private val SyncWorkState.isLive: Boolean
        get() = runState == SyncRunState.Running && stats != null

    private val Counter.processed: Int
        get() = discovered - pending

    /**
     * The SDK rebuilds its counts whenever a sync is suspended and resumed, so a sync without a
     * counter is always a first observation. Holding on a first sample with work keeps the fraction
     * indeterminate until the counts settle; an idle first sample never holds, as idle syncs are silent.
     */
    private fun nextCounter(previous: Counter?, sample: SyncStatsSample): Counter {
        val pending = sample.pendingItems
        return if (previous == null) {
            Counter(
                discovered = pending,
                pending = pending,
                awaitingScanEnd = sample.isScanning || pending > 0,
            )
        } else {
            Counter(
                discovered = previous.discovered + maxOf(0, pending - previous.pending),
                pending = pending,
                awaitingScanEnd = previous.awaitingScanEnd && sample.isScanning,
            )
        }
    }

    /**
     * Interrupted files stay in the denominator so a pause keeps its percentage instead of
     * reading as done, and with nothing live there is nothing to measure, so the fraction is held.
     */
    private fun nextFraction(
        previous: Float?,
        counters: Map<Long, Counter>,
        retiredProcessed: Int,
        interruptions: Map<Long, Interruption>,
    ): Float? {
        if (counters.isEmpty() || counters.values.any { it.awaitingScanEnd }) return previous
        val processed = retiredProcessed + counters.values.sumOf { it.processed }
        val denominator = processed + counters.values.sumOf { it.pending } +
                interruptions.values.sumOf { it.pendingItems }
        if (denominator == 0) return previous
        return maxOf(previous ?: 0f, processed.toFloat() / denominator)
    }

    companion object {
        val EPISODE_IDLE_GRACE = 5.seconds
        val INTERRUPTION_HOLD = 30.minutes
    }
}
