package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.usecase.transfers.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToLastByTagUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferMove
import mega.privacy.android.feature.sync.domain.usecase.progress.ResolveSyncTransferMoveUseCase.ResolvedMove
import javax.inject.Inject

/** Reorders one sync row with one SDK request, resolved against every pending sync row. */
internal class MoveSyncTransferUseCase @Inject constructor(
    private val monitorSyncTransferListUseCase: MonitorSyncTransferListUseCase,
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase,
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase,
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase,
    private val resolveSyncTransferMoveUseCase: ResolveSyncTransferMoveUseCase,
) {

    /**
     * Returns false when the move resolves to a no-op and nothing was sent. A failed request
     * throws. The whole Pending list is read, because an anchor can be a row below the window.
     */
    suspend operator fun invoke(tag: Int, move: SyncTransferMove): Boolean {
        val pending = monitorSyncTransferListUseCase(
            filter = SyncTransferFilter.Pending,
            limit = Int.MAX_VALUE,
        ).first().items
        val resolved = resolveSyncTransferMoveUseCase(tag, move, pending) ?: return false
        when (resolved) {
            ResolvedMove.ToFirst -> moveTransferToFirstByTagUseCase(tag)
            ResolvedMove.ToLast -> moveTransferToLastByTagUseCase(tag)
            is ResolvedMove.Before -> moveTransferBeforeByTagUseCase(tag, resolved.prevTag)
        }
        return true
    }
}
