package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseReasonInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressTimeline
import javax.inject.Inject

/**
 * Returns the timeline after a new input at `nowMillis`: whether it is busy, when a busy phase
 * without work ends by itself, and when the idle stretch started. Later idle inputs keep the
 * same idle start, so a run shows Completed once.
 */
internal class AdvanceSyncProgressTimelineUseCase @Inject constructor(
    private val getSyncProgressPhaseUseCase: GetSyncProgressPhaseUseCase,
) {

    operator fun invoke(
        previous: SyncProgressTimeline?,
        input: SyncProgressInput,
        nowMillis: Long,
    ): SyncProgressTimeline {
        val result = getPhase(input, nowMillis)
        val isBusy = result.phase.isBusy()
        val hasWork = input.runSnapshot.syncs.any { it.hasWork }
        val idleStart = when {
            !input.runSnapshot.isSeeded -> null
            isBusy -> null
            result.phase is SyncProgressPhase.Paused -> null
            result.phase == SyncProgressPhase.NoSyncs -> null
            // An idle cold start, such as a start-up rescan that found nothing, never shows Completed
            previous == null -> null
            previous.isBusy -> busyEndedAt(previous, nowMillis)
            else -> previous.idleStartMillis
        }
        return SyncProgressTimeline(
            input = input,
            isBusy = isBusy,
            busyUntilMillis = if (isBusy && !hasWork) result.reevaluateAtMillis else null,
            idleStartMillis = idleStart,
        )
    }

    // A Checking held by the unknown-stats window may have ended at its cap in a pause that only
    // the status resolution saw, and an idle stretch after a pause never shows Completed
    private fun busyEndedAt(previous: SyncProgressTimeline, now: Long): Long? {
        val endedAt = minOf(now, busyEnd(previous.input, previous.busyUntilMillis ?: now, now))
        val phaseAtEnd = getPhase(previous.input, endedAt).phase
        return endedAt.takeUnless { phaseAtEnd is SyncProgressPhase.Paused }
    }

    // The phase use case re-evaluates at the earliest unknown-stats cap only, so a Checking held
    // by several syncs without stats ends at the first re-evaluation that is no longer busy
    private fun busyEnd(input: SyncProgressInput, from: Long, now: Long): Long {
        var at = from
        while (at < now) {
            val result = getPhase(input, at)
            if (!result.phase.isBusy()) break
            at = result.reevaluateAtMillis ?: now
        }
        return at
    }

    private fun getPhase(input: SyncProgressInput, now: Long) =
        getSyncProgressPhaseUseCase(
            runSnapshot = input.runSnapshot,
            pauseReason = (input.reason as? SyncPauseReasonInput.Known)?.reason,
            nowMillis = now,
        )

    private fun SyncProgressPhase.isBusy() =
        this == SyncProgressPhase.Transferring || this == SyncProgressPhase.Checking
}
