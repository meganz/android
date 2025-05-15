package mega.privacy.android.app.presentation.transfers.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.app.presentation.transfers.view.ACTIVE_TAB_INDEX
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer

/**
 * UI state for Transfers screen.
 *
 * @property selectedTab Selected tab.
 * @property activeTransfers List of in progress transfers.
 * @property isStorageOverQuota Whether the storage is over quota.
 * @property isTransferOverQuota Whether the transfer is over quota.
 * @property areTransfersPaused Whether the transfers are paused.
 * @property completedTransfers List of successfully completed transfers.
 * @property failedTransfers List of cancelled or failed completed transfers.
 */
data class TransfersUiState(
    val selectedTab: Int = ACTIVE_TAB_INDEX,
    val activeTransfers: ImmutableList<InProgressTransfer> = emptyList<InProgressTransfer>().toImmutableList(),
    val isStorageOverQuota: Boolean = false,
    val isTransferOverQuota: Boolean = false,
    val areTransfersPaused: Boolean = false,
    val completedTransfers: ImmutableList<CompletedTransfer> = emptyList<CompletedTransfer>().toImmutableList(),
    val failedTransfers: ImmutableList<CompletedTransfer> = emptyList<CompletedTransfer>().toImmutableList(),
) {

    /**
     * Whether the storage or transfer is over quota.
     */
    val isOverQuota = isStorageOverQuota || isTransferOverQuota
}