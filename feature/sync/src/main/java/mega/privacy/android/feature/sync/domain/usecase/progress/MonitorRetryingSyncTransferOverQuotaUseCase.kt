package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import javax.inject.Inject

/**
 * Monitors whether a live sync transfer row is retrying over the transfer quota. The account
 * blocker misses a quota that only sync transfers have reached, so this row signal fills the gap.
 */
internal class MonitorRetryingSyncTransferOverQuotaUseCase @Inject constructor(
    private val monitorSyncTransferStoreUseCase: MonitorSyncTransferStoreUseCase,
    private val getSyncTransferRowStateUseCase: GetSyncTransferRowStateUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(): Flow<Boolean> = monitorSyncTransferStoreUseCase()
        .map { state -> state.active.values.any { it.isRetryingOverTransferQuota() } }
        .distinctUntilChanged()
        .flowOn(defaultDispatcher)

    // The state check runs first so that the full row rule runs only for retrying rows. The
    // pause and the account blockers are cleared so that only the row's own error decides.
    private fun SyncTransferRow.isRetryingOverTransferQuota(): Boolean =
        entry.state == TransferState.STATE_RETRYING &&
                getSyncTransferRowStateUseCase(
                    row = copy(optimisticPause = null),
                    context = NO_BLOCKERS,
                    directionSpeed = null,
                ).status == SyncTransferStatus.OutOfTransferQuota

    private companion object {
        val NO_BLOCKERS = SyncTransferRowContext(
            areTransfersPaused = false,
            isStorageOverQuota = false,
            isTransferOverQuota = false,
        )
    }
}
