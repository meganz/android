package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.entity.progress.SyncActivity
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Works out the sync progress phase from the sync activity. Never returns Completed: only
 * [ResolveSyncProgressStatusUseCase] does, after a run.
 */
internal class SyncProgressPhaseCalculator @Inject constructor() {

    /**
     * Calculate the phase
     *
     * @param activity The sync activity; a not seeded one gives UpToDate
     * @param pauseReason The condition blocking the syncs, if any
     * @param nowMillis The current time
     */
    fun calculate(activity: SyncActivity, pauseReason: SyncPauseReason?, nowMillis: Long): Result {
        val syncs = activity.syncs
        if (activity.isSeeded && syncs.isEmpty()) return Result(SyncProgressPhase.NoSyncs, null)
        val hasWork = syncs.any { it.hasWork }
        if (hasWork && activity.progress.pendingItems > 0) {
            return Result(SyncProgressPhase.Transferring, null)
        }
        // A sync that just entered Running sends no stats until it has something to say, and an
        // idle one may never send any, so it counts as checking only until the cap
        val unknownStatsUntil = syncs.mapNotNull { sync ->
            sync.runningSinceMillis
                ?.takeIf { sync.runState == SyncRunState.Running && sync.stats == null }
                ?.plus(UNKNOWN_STATS_CAP.inWholeMilliseconds)
                ?.takeIf { it > nowMillis }
        }.minOrNull()
        if (hasWork || unknownStatsUntil != null) {
            return Result(SyncProgressPhase.Checking, unknownStatsUntil)
        }
        val isPaused = (syncs.none { it.runState == SyncRunState.Running } &&
                syncs.any { it.runState == SyncRunState.Suspended }) ||
                activity.progress.interruptedItems > 0
        if (isPaused) return Result(SyncProgressPhase.Paused(pauseCause(syncs, pauseReason)), null)
        return Result(SyncProgressPhase.UpToDate, null)
    }

    // The pause reason describes current conditions, not why a sync was paused, so a pause
    // with no condition is the user's (or a cross-device conflict's, which looks the same)
    private fun pauseCause(
        syncs: List<SyncWorkState>,
        pauseReason: SyncPauseReason?,
    ): SyncPauseCause = when {
        syncs.any { it.runState == SyncRunState.Suspended && it.isStorageOverQuota } ->
            SyncPauseCause.StorageOverQuota

        pauseReason != null -> SyncPauseCause.Condition(pauseReason)
        else -> SyncPauseCause.User
    }

    /**
     * The calculated phase
     *
     * @property phase The phase; never Completed
     * @property reevaluateAtMillis When a Checking phase held by a sync without stats ends, or null
     */
    data class Result(val phase: SyncProgressPhase, val reevaluateAtMillis: Long?)

    companion object {
        /**
         * How long a sync that entered Running without stats counts as checking
         */
        val UNKNOWN_STATS_CAP = 10.seconds
    }
}
