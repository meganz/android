package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transformLatest
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.SystemTime
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseReasonInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressTimeline
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncPauseReasonUseCase
import javax.inject.Inject

/**
 * Monitors the sync episode status: the phase of the current run and its progress.
 * After a busy phase it waits [ResolveSyncProgressStatusUseCase.IDLE_DEBOUNCE] before showing the
 * run as finished, then shows Completed for [ResolveSyncProgressStatusUseCase.COMPLETED_HOLD]
 * when the run processed files.
 */
internal class MonitorSyncProgressStatusUseCase @Inject constructor(
    private val monitorSyncRunSnapshotUseCase: MonitorSyncRunSnapshotUseCase,
    private val monitorSyncPauseReasonUseCase: MonitorSyncPauseReasonUseCase,
    private val advanceSyncProgressTimelineUseCase: AdvanceSyncProgressTimelineUseCase,
    private val resolveSyncProgressStatusUseCase: ResolveSyncProgressStatusUseCase,
    @SystemTime private val currentTimeProvider: () -> Long,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke
     *
     * @return the status, emitted at once when not seeded; distinct
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<SyncProgressStatus> = combine(
        monitorSyncRunSnapshotUseCase(),
        monitorSyncPauseReasonUseCase()
            .distinctUntilChanged()
            .map<SyncPauseReason?, SyncPauseReasonInput> { SyncPauseReasonInput.Known(it) }
            // The reason's first value is sampled about a second late. Only a Paused status waits
            // for it, so the cause never flips from User to a condition right after collection.
            .onStart { emit(SyncPauseReasonInput.Unknown) },
    ) { runSnapshot, reason -> SyncProgressInput(runSnapshot, reason) }
        .scan<SyncProgressInput, SyncProgressTimeline?>(null) { previous, input ->
            advanceSyncProgressTimelineUseCase(previous, input, currentTimeProvider())
        }
        .filterNotNull()
        .transformLatest { latest ->
            var timeline = latest
            while (true) {
                val now = currentTimeProvider()
                val step = resolveSyncProgressStatusUseCase(timeline, now)
                step.status?.let { emit(it) }
                timeline = step.timeline
                val wakeAt = step.wakeAtMillis ?: break
                delay(wakeAt - now)
            }
        }
        .distinctUntilChanged()
        .flowOn(defaultDispatcher)
}
