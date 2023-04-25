package mega.privacy.android.app.main.managerSections

import mega.privacy.android.app.AndroidCompletedTransfer

/**
 * The state regarding completed transfers UI.
 */
sealed class CompletedTransfersState {
    /**
     * Update the completed transfers
     *
     * @property newTransfers  [AndroidCompletedTransfer] List
     */
    data class TransfersUpdated(
        val newTransfers: List<AndroidCompletedTransfer?>,
    ) : CompletedTransfersState()

    /**
     * Update when the transfer is finished
     *
     * @property newTransfers updated [AndroidCompletedTransfer] list
     */
    data class TransferFinishUpdated(
        val newTransfers: List<AndroidCompletedTransfer?>,
    ) : CompletedTransfersState()

    /**
     * Update when the transfer is removed
     *
     * @property index the index of removed item
     * @property newTransfers updated [AndroidCompletedTransfer] list
     */
    data class TransferRemovedUpdated(
        val index: Int,
        val newTransfers: List<AndroidCompletedTransfer?>,
    ) : CompletedTransfersState()

    /**
     * Update when clear completed transfers
     */
    object ClearTransfersUpdated : CompletedTransfersState()

    /**
     * The default state.
     */
    object Default : CompletedTransfersState()
}
