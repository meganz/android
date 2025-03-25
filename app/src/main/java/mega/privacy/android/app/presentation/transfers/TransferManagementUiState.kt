package mega.privacy.android.app.presentation.transfers

import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel.Companion.INVALID_TIMESTAMP
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
 * @property isTransferOverQuota true if the account is in transfer over quota
 * @property isInTransfersSection true if the app is in the transfers section
 * @property transferOverQuotaTimestamp timestamp to avoid show duplicated transfer over quota warnings
 * @property transferOverQuotaWarning true if a transfer over quota warning needs to be notified.
 */
data class TransferManagementUiState(
    val transfersInfo: TransfersInfo = TransfersInfo(),
    val hideTransfersWidget: Boolean = false,
    val lastTransfersCancelled: Boolean = false,
    val isTransferError: Boolean = false,
    val isOnline: Boolean = false,
    val isTransferOverQuota: Boolean = false,
    val isInTransfersSection: Boolean = false,
    val transferOverQuotaTimestamp: Long = INVALID_TIMESTAMP,
    val transferOverQuotaWarning: Boolean = false,
)