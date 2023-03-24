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
     * Update the active transfers
     *
     * @property newTransfers  [MegaTransfer] List
     */
    data class TransfersUpdated(val newTransfers: List<Transfer>) :
        ActiveTransfersState()

    /**
     * Update when the transfer speed changed
     *
     * @property index the index of changed transfer
     * @property updatedTransfer the updated transfer
     * @property newTransfers updated [MegaTransfer] List
     */
    data class TransferUpdated(
        val index: Int,
        val updatedTransfer: Transfer,
        val newTransfers: List<Transfer>,
    ) :
        ActiveTransfersState()

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
    ) :
        ActiveTransfersState()

    /**
     * Update when the new transfer start
     *
     * @property updatedTransfer the updated transfer
     * @property newTransfers updated [MegaTransfer] List
     */
    data class TransferStartUpdated(
        val updatedTransfer: Transfer,
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
     * Update when the transfer status is changed
     *
     * @property index index of status changed item
     * @property transfer status changed item
     */
    data class TransferChangeStatusUpdated(val index: Int, val transfer: Transfer) :
        ActiveTransfersState()

    /**
     * The default state.
     */
    object Default : ActiveTransfersState()
}