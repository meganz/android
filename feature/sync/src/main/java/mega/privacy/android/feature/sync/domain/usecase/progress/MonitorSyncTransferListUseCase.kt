package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import javax.inject.Inject

/**
 * Monitors the sync transfers a [SyncTransferFilter] shows, in desktop order and capped at a
 * window of rows, re-projected when the store, the app-wide pause or the account quota changes.
 */
internal class MonitorSyncTransferListUseCase @Inject constructor(
    private val monitorSyncTransferStoreUseCase: MonitorSyncTransferStoreUseCase,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val monitorSyncProgressBlockersUseCase: MonitorSyncProgressBlockersUseCase,
    private val getSyncTransferListUseCase: GetSyncTransferListUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(
        filter: SyncTransferFilter,
        limit: Int = GetSyncTransferListUseCase.VISIBLE_ROW_LIMIT,
    ): Flow<SyncTransferList> = combine(
        monitorSyncTransferStoreUseCase(),
        monitorPausedTransfersUseCase().distinctUntilChanged(),
        // Stalled-issue refreshes re-emit the blockers; only the quota flags change a row.
        monitorSyncProgressBlockersUseCase()
            .map { QuotaFlags(it.isStorageOverQuota, it.isTransferOverQuota) }
            .distinctUntilChanged(),
    ) { state, areTransfersPaused, quota ->
        getSyncTransferListUseCase(
            state = state,
            filter = filter,
            context = SyncTransferRowContext(
                areTransfersPaused = areTransfersPaused,
                isStorageOverQuota = quota.isStorageOverQuota,
                isTransferOverQuota = quota.isTransferOverQuota,
            ),
            limit = limit,
        )
    }.distinctUntilChanged().flowOn(defaultDispatcher)

    private data class QuotaFlags(
        val isStorageOverQuota: Boolean,
        val isTransferOverQuota: Boolean,
    )
}
