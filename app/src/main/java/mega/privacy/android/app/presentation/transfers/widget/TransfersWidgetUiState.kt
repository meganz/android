package mega.privacy.android.app.presentation.transfers.widget

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetStatus
import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus

/**
 * Transfer management ui state
 *
 * @property transfersInfo
 * there are current transfers or not, in transfers screen for instance
 * @property lastTransfersCancelled flag to indicate that last finished transfers has been cancelled,
 * so transfer status should be [TransfersStatus.Cancelled] until new transfer events are received
 * @property isTransferError true if there is a transfer error and transfers section has not been visited.
 * @property isOnline true if the device is online
 * @property openTransfersSectionEvent event to open the transfers section
 */
data class TransfersWidgetUiState(
    val transfersInfo: TransfersInfo = TransfersInfo(),
    val lastTransfersCancelled: Boolean = false,
    val isTransferError: Boolean = false,
    val isOnline: Boolean = false,
    val openTransfersSectionEvent: StateEvent = consumed
) {
    /**
     * get the status of transfers toolbar widget in a lazy way
     */
    val transfersToolbarWidgetStatus by lazy {
        when (transfersInfo.status) {
            TransfersStatus.Transferring -> TransfersToolbarWidgetStatus.Transferring
            TransfersStatus.Completed -> TransfersToolbarWidgetStatus.Completed
            TransfersStatus.Paused -> TransfersToolbarWidgetStatus.Paused
            TransfersStatus.OverQuota -> TransfersToolbarWidgetStatus.OverQuota
            TransfersStatus.TransferError -> TransfersToolbarWidgetStatus.Error
            else -> TransfersToolbarWidgetStatus.Idle
        }
    }
}