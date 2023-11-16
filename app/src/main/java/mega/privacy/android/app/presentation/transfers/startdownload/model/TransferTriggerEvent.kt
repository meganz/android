package mega.privacy.android.app.presentation.transfers.startdownload.model

import mega.privacy.android.domain.entity.node.Node

/**
 * Event to trigger the start of a transfer
 */
sealed interface TransferTriggerEvent {
    /**
     * Event to start downloading a node for offline use
     * @param node the node to be saved offline
     */
    data class StartDownloadForOffline(val node: Node) : TransferTriggerEvent

    /**
     * Event to start downloading a list of nodes to download folder
     * @param nodes list of nodes to be downloaded, they should belong to the same parent folder
     */
    data class StartDownloadNode(val nodes: List<Node>) : TransferTriggerEvent
}