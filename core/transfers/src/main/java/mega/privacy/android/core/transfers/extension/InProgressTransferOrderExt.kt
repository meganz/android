package mega.privacy.android.core.transfers.extension

import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState

/**
 * Whether this transfer is underway, as opposed to waiting its turn in the queue.
 *
 * Paused counts as underway so that pausing a row does not move it. Pausing the whole queue then
 * leaves every transfer in the same group, falling back to plain priority order.
 */
val InProgressTransfer.isUnderway: Boolean
    get() = state in underwayStates

/**
 * Orders transfers so the ones underway come first, each group ordered by SDK priority.
 *
 * Uploads are queued in several pools defined by size range, so plain priority order scatters the
 * transfers that are actually progressing throughout the whole list.
 */
fun Collection<InProgressTransfer>.sortedByUnderwayThenPriority(): List<InProgressTransfer> =
    sortedWith(compareBy<InProgressTransfer>({ !it.isUnderway }, { it.priority }))

private val underwayStates = setOf(
    TransferState.STATE_ACTIVE,
    TransferState.STATE_RETRYING,
    TransferState.STATE_COMPLETING,
    TransferState.STATE_PAUSED,
)
