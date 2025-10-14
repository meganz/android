package mega.privacy.android.app.presentation.transfers.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * UI state for Transfers screen.
 *
 * @property selectedTab Selected tab.
 * @property activeTransfers List of in progress transfers.
 * @property selectedActiveTransfersIds List of selected in progress transfers ids. If not null, even empty, indicates selected mode is on.
 * @property isStorageOverQuota Whether the storage is over quota.
 * @property isTransferOverQuota Whether the transfer is over quota.
 * @property quotaWarning Quota warning, can be null if no quota warning is present.
 * @property areTransfersPaused Whether the transfers are paused.
 * @property completedTransfers List of successfully completed transfers.
 * @property selectedCompletedTransfersIds List of selected completed transfers. If not null, even empty, indicates selected mode is on.
 * @property failedTransfers List of cancelled or failed completed transfers.
 * @property selectedFailedTransfersIds List of selected failed or cancelled transfers. If not null, even empty, indicates selected mode is on.
 * @property startEvent event to start a new transfer
 * @property transfersPendingToCancel Map of transfers pending to cancel from swipe gesture.
 */
data class TransfersUiState(
    val selectedTab: Int = ACTIVE_TAB_INDEX,
    val activeTransfers: List<InProgressTransfer> = listOf(),
    val selectedActiveTransfersIds: List<Long>? = null,
    val isStorageOverQuota: Boolean = false,
    val isTransferOverQuota: Boolean = false,
    val quotaWarning: QuotaWarning? = null,
    val areTransfersPaused: Boolean = false,
    val completedTransfers: List<CompletedTransfer> = listOf(),
    val selectedCompletedTransfersIds: List<Int>? = null,
    val failedTransfers: List<CompletedTransfer> = listOf(),
    val selectedFailedTransfersIds: List<Int>? = null,
    val startEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val transfersPendingToCancel: Map<Long, TransferPendingToCancel> = emptyMap(),
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
 * Quota warning.
 */
sealed class QuotaWarning {
    /**
     * Storage over quota warning.
     */
    data object Storage : QuotaWarning()

    /**
     * Transfer over quota warning.
     */
    data object Transfer : QuotaWarning()

    /**
     * Both storage and transfer over quota warning.
     */
    data object StorageAndTransfer : QuotaWarning()
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