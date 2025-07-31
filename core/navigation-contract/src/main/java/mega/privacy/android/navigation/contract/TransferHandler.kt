package mega.privacy.android.navigation.contract

import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Interface for handling transfer events in the navigation system.
 * This allows components to trigger transfer events through the navigation hierarchy.
 */
interface TransferHandler {
    /**
     * Set a transfer event to be processed.
     *
     * @param event The transfer trigger event to be set
     */
    fun setTransferEvent(event: TransferTriggerEvent)
} 