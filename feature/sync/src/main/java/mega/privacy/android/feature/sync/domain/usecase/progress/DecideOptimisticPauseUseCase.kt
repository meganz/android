package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow

/**
 * Decides a tracked row's optimistic pause state for a pause or resume request, or for the revert
 * of a failed one when the request is null. A COMPLETING row cannot be paused any more, so a
 * request leaves it as it is. Returns the replaced row, or null when the row stays.
 */
internal class DecideOptimisticPauseUseCase @Inject constructor() {

    operator fun invoke(row: SyncTransferRow, requestedPause: Boolean?): SyncTransferRow? {
        val next = if (requestedPause == null) {
            null
        } else {
            if (row.entry.state == TransferState.STATE_COMPLETING) return null
            val isAlreadyThere = if (requestedPause) {
                row.entry.state == TransferState.STATE_PAUSED
            } else {
                row.entry.state != TransferState.STATE_PAUSED
            }
            // A pause on a row that is already paused produces no SDK event, so it must not stay optimistic.
            if (isAlreadyThere && row.optimisticPause != !requestedPause) null else requestedPause
        }
        return if (next == row.optimisticPause) null else row.copy(optimisticPause = next)
    }
}
