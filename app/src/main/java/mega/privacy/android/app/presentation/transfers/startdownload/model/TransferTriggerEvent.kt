package mega.privacy.android.app.presentation.transfers.startdownload.model

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Event to trigger the start of a transfer
 */
sealed interface TransferTriggerEvent {

    /**
     * nodes to be transferred
     */
    val nodes: List<TypedNode>


    /**
     * Event to start downloading a node for offline use
     * @param node the node to be saved offline
     */
    data class StartDownloadForOffline(val node: TypedNode?) : TransferTriggerEvent {
        override val nodes = node?.let { listOf(node) } ?: emptyList()
    }


    /**
     * Event to start downloading a list of nodes to download folder
     * @param nodes list of nodes to be downloaded, they should belong to the same parent folder
     */
    data class StartDownloadNode(override val nodes: List<TypedNode>) : TransferTriggerEvent

    /**
     * Event to start downloading node for preview
     *
     * @param node the node to be downloaded for preview
     */
    data class StartDownloadForPreview(val node: TypedNode?) : TransferTriggerEvent {
        override val nodes = node?.let { listOf(node) } ?: emptyList()
    }

}