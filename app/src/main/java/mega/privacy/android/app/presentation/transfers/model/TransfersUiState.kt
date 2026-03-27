package mega.privacy.android.app.presentation.transfers.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.shared.account.overquota.OverQuotaStatus

/**
 * UI state for Transfers screen.
 *
 * @property activeTransfers List of in progress transfers.
 * @property selectedActiveTransfersIds List of selected in progress transfers ids. If not null, even empty, indicates selected mode is on.
 * @property overQuotaStatus Over-quota status (storage/transfer, blocking vs non-blocking).
 * @property areTransfersPaused Whether the transfers are paused.
 * @property completedTransfers List of successfully completed transfers.
 * @property selectedCompletedTransfersIds List of selected completed transfers. If not null, even empty, indicates selected mode is on.
 * @property failedTransfers List of cancelled or failed completed transfers.
 * @property selectedFailedTransfersIds List of selected failed or cancelled transfers. If not null, even empty, indicates selected mode is on.
 * @property startEvent event to start a new transfer
 * @property transfersPendingToCancel Map of transfers pending to cancel from swipe gesture.
 * @property hasInternetConnection true if there is Internet connection
 */
data class TransfersUiState(
    val activeTransfers: List<InProgressTransfer> = listOf(),
    val selectedActiveTransfersIds: List<Long>? = null,
    val overQuotaStatus: OverQuotaStatus = OverQuotaStatus(),
    val areTransfersPaused: Boolean = false,
    val completedTransfers: List<CompletedTransfer> = listOf(),
    val selectedCompletedTransfersIds: List<Int>? = null,
    val failedTransfers: List<CompletedTransfer> = listOf(),
    val selectedFailedTransfersIds: List<Int>? = null,
    val startEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val transfersPendingToCancel: Map<Long, TransferPendingToCancel> = emptyMap(),
    val hasInternetConnection: Boolean = true,
    val transferInError: Boolean = false,
) {

    /**
     * true if it's in select mode, false otherwise
     */
    val isInSelectTransfersMode =
        selectedActiveTransfersIds != null || selectedCompletedTransfersIds != null || selectedFailedTransfersIds != null


    /**
     * current selected transfers amount
     */
    val selectedTransfersAmount = selectedActiveTransfersIds?.size
        ?: selectedCompletedTransfersIds?.size
        ?: selectedFailedTransfersIds?.size
        ?: 0

    /**
     * All active transfers are selected
     */
    val areAllActiveTransfersSelected by lazy {
        selectedActiveTransfersIds?.containsAll(activeTransfers.map { it.uniqueId }) == true
    }

    /**
     * All completed transfers are selected
     */
    val areAllCompletedTransfersSelected by lazy {
        selectedCompletedTransfersIds?.containsAll(completedTransfers.map { it.id }) == true
    }

    /**
     * All completed transfers are selected
     */
    val areAllFailedTransfersSelected by lazy {
        selectedFailedTransfersIds?.containsAll(failedTransfers.map { it.id }) == true
    }
}

/**
 * Data class storing info required for cancelling a transfer from swipe gesture.
 *
 * @property tag Transfer tag.
 * @property isPaused Paused state when the swipe gesture takes place.
 *                    Used to restore it if undo action is pressed.
 */
data class TransferPendingToCancel(
    val tag: Int,
    val isPaused: Boolean,
)