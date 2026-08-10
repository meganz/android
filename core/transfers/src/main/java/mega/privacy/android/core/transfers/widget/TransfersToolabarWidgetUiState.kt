package mega.privacy.android.core.transfers.widget

import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetStatus

/**
 * Transfer management ui state
 *
 * @property totalSizeAlreadyTransferred
 * @property totalSizeToTransfer
 * @property status [TransfersToolbarWidgetStatus], null means that initial status is still unknown
 * @property lastTransfersCancelled flag to indicate that last finished transfers has been cancelled
 * @property isTransferError true if there is a transfer error and transfers section has not been visited.
 * @property isUserLoggedIn
 */
data class TransfersToolabarWidgetUiState(
    val status: TransfersToolbarWidgetStatus? = null,
    val totalSizeAlreadyTransferred: Long = 0L,
    val totalSizeToTransfer: Long = 0L,
    val lastTransfersCancelled: Boolean = false,
    val isTransferError: Boolean = false,
    val isUserLoggedIn: Boolean = false
)