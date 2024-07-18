package mega.privacy.android.app.presentation.transfers.attach

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Attach node to chat event
 *
 */
sealed class NodeAttachmentEvent {
    /**
     * Show over disk quota paywall
     *
     */
    data object ShowOverDiskQuotaPaywall : NodeAttachmentEvent()

    /**
     * Select chat
     *
     * @property nodeIds List of node ids to attach
     */
    data class SelectChat(val nodeIds: List<NodeId>) : NodeAttachmentEvent()

    /**
     * Attach node
     *
     * @property nodeIds
     */
    data class AttachNode(val nodeIds: List<NodeId>) : NodeAttachmentEvent()

    /**
     * Attach node success
     *
     * @property chatIds
     */
    data class AttachNodeSuccess(val chatIds: List<Long>) : NodeAttachmentEvent()
}