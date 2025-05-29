package mega.privacy.android.app.presentation.transfers.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer

/**
 * UI state for Transfers screen.
 *
 * @property selectedTab Selected tab.
 * @property activeTransfers List of in progress transfers.
 * @property selectedActiveTransfers List of selected in progress transfers. If not null, even empty, indicates selected mode is on.
 * @property isStorageOverQuota Whether the storage is over quota.
 * @property isTransferOverQuota Whether the transfer is over quota.
 * @property areTransfersPaused Whether the transfers are paused.
 * @property completedTransfers List of successfully completed transfers.
 * @property failedTransfers List of cancelled or failed completed transfers.
 * @property startEvent event to start a new transfer
 * @property readRetryError Null if there is no error, read retry error count otherwise
 */
data class TransfersUiState(
    val selectedTab: Int = ACTIVE_TAB_INDEX,
    val activeTransfers: ImmutableList<InProgressTransfer> = emptyList<InProgressTransfer>().toImmutableList(),
    val selectedActiveTransfers: ImmutableList<InProgressTransfer>? = null,
    val isStorageOverQuota: Boolean = false,
    val isTransferOverQuota: Boolean = false,
    val areTransfersPaused: Boolean = false,
    val completedTransfers: ImmutableList<CompletedTransfer> = emptyList<CompletedTransfer>().toImmutableList(),
    val failedTransfers: ImmutableList<CompletedTransfer> = emptyList<CompletedTransfer>().toImmutableList(),
    val startEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val readRetryError: Int? = null,
) {

    /**
     * Whether the storage or transfer is over quota.
     */
    val isOverQuota = isStorageOverQuota || isTransferOverQuota

    /**
     * @return true if it's in select mode, false otherwise
     */
    val isInSelectActiveTransfersMode = selectedActiveTransfers != null

    /**
     * All active transfers are selected
     */
    val areAllActiveTransfersSelected = activeTransfers == selectedActiveTransfers
}