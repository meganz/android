package mega.privacy.android.core.transfers.widget

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetStatus

/**
 * Transfer management ui state
 *
 * @property totalSizeAlreadyTransferred
 * @property totalSizeToTransfer
 * @property status
 * @property lastTransfersCancelled flag to indicate that last finished transfers has been cancelled,
 * so transfer status should be [TransfersStatus.Cancelled] until new transfer events are received
 * @property isTransferError true if there is a transfer error and transfers section has not been visited.
 * @property isOnline true if the device is online
 * @property isUserLoggedIn
 */
data class TransfersToolabarWidgetUiState(
    val status: TransfersToolbarWidgetStatus = TransfersToolbarWidgetStatus.Idle,
    val totalSizeAlreadyTransferred: Long = 0L,
    val totalSizeToTransfer: Long = 0L,
    val lastTransfersCancelled: Boolean = false,
    val isTransferError: Boolean = false,
    val isOnline: Boolean = false,
    val isUserLoggedIn: Boolean = false
)