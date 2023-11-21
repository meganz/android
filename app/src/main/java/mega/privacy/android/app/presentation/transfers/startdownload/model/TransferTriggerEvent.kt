package mega.privacy.android.app.presentation.transfers.startdownload.model

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId

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
     * Event to start downloading a node for offline use, it's preferred to use [StartDownloadForOffline] if the node is already fetched.
     * @param nodeId the node id of the node to be saved offline
     */
    data class StartDownloadForOfflineWithId(val nodeId: NodeId) : TransferTriggerEvent

    /**
     * Event to start downloading a list of nodes to download folder
     * @param nodes list of nodes to be downloaded, they should belong to the same parent folder
     */
    data class StartDownloadNode(val nodes: List<Node>) : TransferTriggerEvent

    /**
     * Event to start downloading a list of nodes to download folder, it's preferred to use [StartDownloadNode] if the nodes are already fetched.
     * @param nodeIds list of node ids of the nodes to be downloaded, they should belong to the same parent folder
     */
    data class StartDownloadNodeWithId(val nodeIds: List<NodeId>) : TransferTriggerEvent
}