package mega.privacy.android.feature.sync.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferSpeeds
import mega.privacy.android.feature.sync.domain.usecase.progress.GetSyncTransferRowStateUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorRetryingSyncTransferOverQuotaUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncProgressBlockersUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncProgressStatusUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncTransferListUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncTransferSpeedsUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.band
import mega.privacy.android.feature.sync.ui.mapper.progress.SyncProgressCompletedBodyMapper
import mega.privacy.android.feature.sync.ui.mapper.progress.SyncProgressUiStateMapper
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressCompletedBody
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * Exposes the typed state of the sync progress screen. The inputs are collected only while
 * [uiState] has subscribers, and the mapping runs on the default dispatcher. An upstream failure is
 * logged, and [uiState] then keeps its last value and stops updating.
 */
@HiltViewModel
internal class SyncProgressViewModel @Inject constructor(
    private val monitorSyncProgressStatusUseCase: MonitorSyncProgressStatusUseCase,
    private val monitorSyncTransferListUseCase: MonitorSyncTransferListUseCase,
    private val monitorSyncProgressBlockersUseCase: MonitorSyncProgressBlockersUseCase,
    private val monitorSyncTransferSpeedsUseCase: MonitorSyncTransferSpeedsUseCase,
    private val getSyncTransferRowStateUseCase: GetSyncTransferRowStateUseCase,
    private val monitorRetryingSyncTransferOverQuotaUseCase: MonitorRetryingSyncTransferOverQuotaUseCase,
    private val syncProgressCompletedBodyMapper: SyncProgressCompletedBodyMapper,
    private val syncProgressUiStateMapper: SyncProgressUiStateMapper,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // No root-node signal is wired yet, so both bodies treat the root node as ready.
    private val isRootNodeReady = true

    val uiState: StateFlow<SyncProgressUiState> by lazy(LazyThreadSafetyMode.NONE) {
        monitorProgressInputs()
            .map { it.toUiState() }
            .flowOn(defaultDispatcher)
            .catch { Timber.e(it, "Failed to monitor sync progress") }
            .asUiStateFlow(viewModelScope, SyncProgressUiState())
    }

    private fun monitorProgressInputs(): Flow<ProgressInputs> = combine(
        monitorSyncProgressStatusUseCase(),
        monitorSyncTransferListUseCase(SyncTransferFilter.Pending).nullUntilFirst(),
        monitorCompletedInputs(),
        monitorSyncProgressBlockersUseCase(),
        monitorSyncTransferSpeedsUseCase().nullUntilFirst(),
    ) { status, pendingList, completed, blockers, speeds ->
        ProgressInputs(
            status = status,
            pendingList = pendingList,
            completedList = completed.list,
            completedBody = completed.body,
            blockers = blockers,
            speeds = speeds,
        )
    }.combine(
        // No retrying row is known until the store emits, so `combine` must not wait for it.
        monitorRetryingSyncTransferOverQuotaUseCase().onStart { emit(false) }
    ) { inputs, value ->
        inputs.copy(isRetryingTransferOverQuota = value)
    }

    /**
     * Maps the Completed body only when the Completed list changes, so the other inputs, which
     * emit far more often, do not remap up to the full window of completed rows.
     */
    private fun monitorCompletedInputs(): Flow<CompletedInputs> =
        monitorSyncTransferListUseCase(SyncTransferFilter.Completed)
            .nullUntilFirst()
            .distinctUntilChanged()
            .map { list ->
                CompletedInputs(
                    list = list,
                    body = syncProgressCompletedBodyMapper(list = list, isRootNodeReady = isRootNodeReady),
                )
            }

    private fun ProgressInputs.toUiState(): SyncProgressUiState {
        val activeLists = listOfNotNull(pendingList)
        return syncProgressUiStateMapper(
            status = status,
            pendingList = pendingList,
            completedList = completedList,
            completedBody = completedBody,
            blockers = blockers,
            isRootNodeReady = isRootNodeReady,
            rowStates = rowStatesOf(activeLists, blockers, speeds),
            bands = bandsOf(activeLists),
            isRetryingTransferOverQuota = isRetryingTransferOverQuota,
        )
    }

    /**
     * Returns one state per active row of every list the screen maps. The direction speed is the
     * value of the row's own direction, or null while it is not known, so the row speed is then
     * not capped.
     */
    private fun rowStatesOf(
        lists: List<SyncTransferList>,
        blockers: SyncProgressBlockers,
        speeds: SyncTransferSpeeds?,
    ): Map<Int, SyncTransferRowState> {
        val context = SyncTransferRowContext(
            areTransfersPaused = blockers.areTransfersPaused,
            isStorageOverQuota = blockers.isStorageOverQuota,
            isTransferOverQuota = blockers.isTransferOverQuota,
        )
        return lists.flatMap { it.items }
            .filterIsInstance<SyncTransferListItem.Active>()
            .associate { item ->
                val directionSpeed = when (item.row.entry.direction) {
                    SyncTransferDirection.Upload -> speeds?.uploadSpeed
                    SyncTransferDirection.Download -> speeds?.downloadSpeed
                }
                item.tag to getSyncTransferRowStateUseCase(item.row, context, directionSpeed)
            }
    }

    /** Returns the band of each active row of [lists]: `item.band`, the only status read here. */
    private fun bandsOf(lists: List<SyncTransferList>): Map<Int, Int> = lists.flatMap { it.items }
        .filterIsInstance<SyncTransferListItem.Active>()
        .associate { it.tag to it.band }

    /** Emits null first, so that `combine` does not wait for a flow that has not emitted yet. */
    private fun <T : Any> Flow<T>.nullUntilFirst(): Flow<T?> = map<T, T?> { it }.onStart { emit(null) }

    private data class CompletedInputs(
        val list: SyncTransferList?,
        val body: SyncProgressCompletedBody,
    )

    private data class ProgressInputs(
        val status: SyncProgressStatus,
        val pendingList: SyncTransferList?,
        val completedList: SyncTransferList?,
        val completedBody: SyncProgressCompletedBody,
        val blockers: SyncProgressBlockers,
        val speeds: SyncTransferSpeeds?,
        val isRetryingTransferOverQuota: Boolean = false,
    )
}
