package mega.privacy.android.app.presentation.transfers.startdownload.model

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Event to trigger the start of a transfer
 */
sealed interface TransferTriggerEvent {
    /**
     * Event to start downloading a node for offline use
     * @param typedNode the node to be saved offline
     */
    data class StartDownloadForOffline(val typedNode: TypedNode) : TransferTriggerEvent

    /**
     * Event to start downloading a list of nodes to download folder
     * @param typedNodes list of nodes to be downloaded, they should belong to the same parent folder
     */
    data class StartDownloadNode(val typedNodes: List<TypedNode>) : TransferTriggerEvent
}