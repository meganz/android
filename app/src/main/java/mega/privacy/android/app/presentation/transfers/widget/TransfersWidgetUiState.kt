package mega.privacy.android.app.presentation.transfers.widget

import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus

/**
 * Transfer management ui state
 *
 * @property transfersInfo
 * @property hideTransfersWidget true if transfers widget should be forced to be hidden regardless of whether
 * there are current transfers or not, in transfers screen for instance
 * @property lastTransfersCancelled flag to indicate that last finished transfers has been cancelled,
 * so transfer status should be [TransfersStatus.Cancelled] until new transfer events are received
 * @property isTransferError true if there is a transfer error and transfers section has not been visited.
 * @property isOnline true if the device is online
 */
data class TransfersWidgetUiState(
    val transfersInfo: TransfersInfo = TransfersInfo(),
    val hideTransfersWidget: Boolean = false,
    val lastTransfersCancelled: Boolean = false,
    val isTransferError: Boolean = false,
    val isOnline: Boolean = false,
)