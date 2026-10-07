package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseReasonInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStep
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressTimeline
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Resolves the status of a timeline at `nowMillis`, and when to resolve it again without a new
 * input. After a busy phase it waits [IDLE_DEBOUNCE] before showing the run as finished, then
 * shows Completed for [COMPLETED_HOLD] when the run processed files.
 */
internal class ResolveSyncProgressStatusUseCase @Inject constructor(
    private val syncProgressPhaseCalculator: SyncProgressPhaseCalculator,
) {

    operator fun invoke(timeline: SyncProgressTimeline, nowMillis: Long): SyncProgressStep {
        val activity = timeline.input.activity
        if (!activity.isSeeded) {
            val status = SyncProgressStatus(
                isSeeded = false,
                phase = SyncProgressPhase.UpToDate,
                progress = activity.progress,
            )
            return SyncProgressStep(status = status, timeline = timeline, wakeAtMillis = null)
        }
        val result = syncProgressPhaseCalculator.calculate(
            activity = activity,
            pauseReason = (timeline.input.reason as? SyncPauseReasonInput.Known)?.reason,
            nowMillis = nowMillis,
        )
        return when (val phase = result.phase) {
            is SyncProgressPhase.Paused -> SyncProgressStep(
                status = status(timeline, phase)
                    .takeIf { timeline.input.reason is SyncPauseReasonInput.Known },
                timeline = timeline,
                wakeAtMillis = null,
            )

            SyncProgressPhase.UpToDate -> {
                // A Checking held only by the unknown-stats window ends here, with no new input
                val idleTimeline = if (timeline.idleStartMillis == null && timeline.isBusy) {
                    timeline.copy(idleStartMillis = nowMillis)
                } else {
                    timeline
                }
                resolveIdle(idleTimeline, nowMillis)
            }

            else -> SyncProgressStep(
                status = status(timeline, phase),
                timeline = timeline,
                wakeAtMillis = result.reevaluateAtMillis,
            )
        }
    }

    private fun resolveIdle(timeline: SyncProgressTimeline, now: Long): SyncProgressStep {
        val idleStart = timeline.idleStartMillis ?: return SyncProgressStep(
            status = status(timeline, SyncProgressPhase.UpToDate),
            timeline = timeline,
            wakeAtMillis = null,
        )
        val debounceEnd = idleStart + IDLE_DEBOUNCE.inWholeMilliseconds
        val holdEnd = debounceEnd + COMPLETED_HOLD.inWholeMilliseconds
        return when {
            // Emit nothing: the last status stays, so a gap between batches never flashes Up to date
            now < debounceEnd -> SyncProgressStep(
                status = null,
                timeline = timeline,
                wakeAtMillis = debounceEnd,
            )

            now < holdEnd && timeline.input.activity.progress.processedItems > 0 ->
                SyncProgressStep(
                    status = status(timeline, SyncProgressPhase.Completed),
                    timeline = timeline,
                    wakeAtMillis = holdEnd,
                )

            else -> SyncProgressStep(
                status = status(timeline, SyncProgressPhase.UpToDate),
                timeline = timeline,
                wakeAtMillis = null,
            )
        }
    }

    private fun status(timeline: SyncProgressTimeline, phase: SyncProgressPhase) =
        SyncProgressStatus(
            isSeeded = true,
            phase = phase,
            progress = timeline.input.activity.progress,
        )

    companion object {
        /**
         * How long the syncs must stay idle after a busy phase before the run counts as finished
         */
        val IDLE_DEBOUNCE = 2.seconds

        /**
         * How long Completed shows after a run that processed files
         */
        val COMPLETED_HOLD = 4.seconds
    }
}
