package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.SystemTime
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineSignals
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.extension.throttleLatest
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Monitors the sync status for the whole app (desktop's status chain). Inputs are throttled to
 * one calculation per [THROTTLE]; a time-based rule is recomputed when it changes.
 */
internal class MonitorSyncEngineStatusUseCase @Inject constructor(
    private val monitorSyncRunSnapshotUseCase: MonitorSyncRunSnapshotUseCase,
    private val monitorSyncProgressStatusUseCase: MonitorSyncProgressStatusUseCase,
    private val monitorSyncProgressBlockersUseCase: MonitorSyncProgressBlockersUseCase,
    private val monitorSyncEngineSignalsUseCase: MonitorSyncEngineSignalsUseCase,
    private val monitorSyncTransferStoreUseCase: MonitorSyncTransferStoreUseCase,
    private val getSyncEngineStateUseCase: GetSyncEngineStateUseCase,
    @SystemTime private val currentTimeProvider: () -> Long,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke
     *
     * @return the state, distinct; emits once every input has emitted, and is NotSeeded until
     * all of them are seeded
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<SyncEngineState> {
        val runSnapshot = monitorSyncRunSnapshotUseCase()
        // A distinct Boolean keeps stats samples from restarting the shared waiting poll.
        val signals = runSnapshot
            .map { it.syncs.any { sync -> sync.runState == SyncRunState.Running } }
            .distinctUntilChanged()
            .flatMapLatest { isAnyRunning ->
                if (isAnyRunning) {
                    monitorSyncEngineSignalsUseCase()
                } else {
                    flowOf(SyncEngineSignals.NotWaiting)
                }
            }
        val store = monitorSyncTransferStoreUseCase()
            .map { StoreInputs(it.isSeeded, it.counts.pendingSyncTransfers) }
            .distinctUntilChanged()
        return combine(
            runSnapshot,
            monitorSyncProgressStatusUseCase(),
            monitorSyncProgressBlockersUseCase(),
            signals,
            store,
        ) { a, s, b, sig, st -> Snapshot(a, s, b, sig, st) }
            .throttleLatest(THROTTLE)
            .transformLatest { snapshot ->
                var state = getState(snapshot)
                emit(state)
                while (true) {
                    val wakeAt = state.reevaluateAtMillis ?: break
                    delay((wakeAt - currentTimeProvider()).coerceAtLeast(0))
                    state = getState(snapshot)
                    emit(state)
                }
            }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)
    }

    private fun getState(snapshot: Snapshot) = getSyncEngineStateUseCase(
        GetSyncEngineStateUseCase.Inputs(
            runSnapshot = snapshot.runSnapshot,
            status = snapshot.status,
            blockers = snapshot.blockers,
            signals = snapshot.signals,
            isStoreSeeded = snapshot.store.isSeeded,
            pendingSyncTransfers = snapshot.store.pendingSyncTransfers,
            nowMillis = currentTimeProvider(),
        )
    )

    private data class StoreInputs(val isSeeded: Boolean, val pendingSyncTransfers: Int)

    private data class Snapshot(
        val runSnapshot: SyncRunSnapshot,
        val status: SyncProgressStatus,
        val blockers: SyncProgressBlockers,
        val signals: SyncEngineSignals,
        val store: StoreInputs,
    )

    companion object {
        /** At most one calculation per window, as desktop's status recompute. */
        val THROTTLE = 200.milliseconds
    }
}
