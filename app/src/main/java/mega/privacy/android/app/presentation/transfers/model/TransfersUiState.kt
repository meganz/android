package mega.privacy.android.app.presentation.transfers.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.domain.entity.transfer.InProgressTransfer

/**
 * UI state for Transfers screen.
 *
 * @property inProgressTransfers List of in progress transfers.
 * @property isStorageOverQuota Whether the storage is over quota.
 * @property isTransferOverQuota Whether the transfer is over quota.
 * @property areTransfersPaused Whether the transfers are paused.
 */
data class TransfersUiState(
    val inProgressTransfers: ImmutableList<InProgressTransfer> = emptyList<InProgressTransfer>().toImmutableList(),
    val isStorageOverQuota: Boolean = false,
    val isTransferOverQuota: Boolean = false,
    val areTransfersPaused: Boolean = false,
) {

    /**
     * Whether the storage or transfer is over quota.
     */
    val isOverQuota = isStorageOverQuota || isTransferOverQuota
}