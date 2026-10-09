package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow

/** Merges a newer non-terminal entry into a tracked row, keeping the row's media type. */
internal class MergeSyncTransferRowUseCase @Inject constructor() {

    operator fun invoke(stored: SyncTransferRow, entry: SyncTransferEntry): SyncTransferRow = stored.copy(
        entry = mergedEntry(stored.entry, entry),
        optimisticPause = resolvedPause(stored.optimisticPause, entry.state),
    )

    private fun mergedEntry(stored: SyncTransferEntry, entry: SyncTransferEntry): SyncTransferEntry {
        // Desktop rebuilds bytes from every event; a retry restarting at zero must not move progress back.
        val keepsBytes = entry.totalBytes == stored.totalBytes && entry.transferredBytes < stored.transferredBytes
        return if (keepsBytes) entry.copy(transferredBytes = stored.transferredBytes) else entry
    }

    // A stale ACTIVE after a pause must not undo the pause, so only a settled state clears it.
    private fun resolvedPause(optimisticPause: Boolean?, state: TransferState): Boolean? = when (optimisticPause) {
        null -> null
        true -> when (state) {
            TransferState.STATE_PAUSED, TransferState.STATE_COMPLETING -> null
            else -> true
        }

        false -> if (state == TransferState.STATE_PAUSED) false else null
    }
}
