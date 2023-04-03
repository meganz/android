package mega.privacy.android.app.main.managerSections

import mega.privacy.android.domain.entity.transfer.Transfer
import nz.mega.sdk.MegaTransfer

/**
 * The state regarding active transfers UI.
 */
sealed class ActiveTransfersState {

    /**
     * Update the get more quota view visibility
     *
     * @property isVisible true is visible, otherwise is gone.
     */
    data class GetMoreQuotaViewVisibility(val isVisible: Boolean) : ActiveTransfersState()

    /**
     * Update when transfer is finished
     *
     * @property success the
     * @property pos the pos of movement finished
     * @property newTransfers updated [MegaTransfer] List
     */
    data class TransferMovementFinishedUpdated(
        val success: Boolean,
        val pos: Int,
        val newTransfers: List<Transfer>,
    ) : ActiveTransfersState()

    /**
     * Update when the new transfer start
     *
     * @property index the index of finished transfer
     * @property newTransfers updated [MegaTransfer] List
     */
    data class TransferFinishedUpdated(val index: Int, val newTransfers: List<Transfer>) :
        ActiveTransfersState()

    /**
     * The default state.
     */
    object Default : ActiveTransfersState()
}