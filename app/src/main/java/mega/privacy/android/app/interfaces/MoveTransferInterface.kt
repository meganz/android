package mega.privacy.android.app.interfaces

interface MoveTransferInterface {
    /**
     * Reverts the priority change of a transfer on UI due to an error applying the change.
     * In short, applies a new movement to set the transfer in the right position by its priority.
     *
     * @param transferTag Identifier of the transfer.
     */
    fun movementFailed(transferTag: Int)

    /**
     * Notifies the change of priority of a transfer finished successfully.
     * Updates the transfer on UI.
     *
     * @param transferTag Identifier of the transfer.
     */
    fun movementSuccess(transferTag: Int)
}