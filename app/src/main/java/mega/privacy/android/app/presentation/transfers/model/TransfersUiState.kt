package mega.privacy.android.app.presentation.transfers.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer

/**
 * UI state for Transfers screen.
 *
 * @property selectedTab Selected tab.
 * @property activeTransfers List of in progress transfers.
 * @property selectedActiveTransfersIds List of selected in progress transfers ids. If not null, even empty, indicates selected mode is on.
 * @property isStorageOverQuota Whether the storage is over quota.
 * @property isTransferOverQuota Whether the transfer is over quota.
 * @property areTransfersPaused Whether the transfers are paused.
 * @property completedTransfers List of successfully completed transfers.
 * @property completedTransfersPaths Map of completed transfer
 * @property selectedCompletedTransfersIds List of selected completed transfers. If not null, even empty, indicates selected mode is on.
 * @property failedTransfers List of cancelled or failed completed transfers.
 * @property selectedFailedTransfersIds List of selected failed or cancelled transfers. If not null, even empty, indicates selected mode is on.
 * @property startEvent event to start a new transfer
 * @property readRetryError Null if there is no error, read retry error count otherwise
 */
data class TransfersUiState(
    val selectedTab: Int = ACTIVE_TAB_INDEX,
    val activeTransfers: ImmutableList<InProgressTransfer> = emptyList<InProgressTransfer>().toImmutableList(),
    val selectedActiveTransfersIds: ImmutableList<Long>? = null,
    val isStorageOverQuota: Boolean = false,
    val isTransferOverQuota: Boolean = false,
    val areTransfersPaused: Boolean = false,
    val completedTransfers: ImmutableList<CompletedTransfer> = emptyList<CompletedTransfer>().toImmutableList(),
    val completedTransfersPaths: ImmutableMap<Int, String> = emptyMap<Int, String>().toImmutableMap(),
    val selectedCompletedTransfersIds: ImmutableList<Int>? = null,
    val failedTransfers: ImmutableList<CompletedTransfer> = emptyList<CompletedTransfer>().toImmutableList(),
    val selectedFailedTransfersIds: ImmutableList<Int>? = null,
    val startEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val readRetryError: Int? = null,
) {

    /**
     * Whether the storage or transfer is over quota.
     */
    val isOverQuota = isStorageOverQuota || isTransferOverQuota

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