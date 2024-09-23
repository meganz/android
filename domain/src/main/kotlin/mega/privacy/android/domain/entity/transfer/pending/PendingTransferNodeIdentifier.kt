package mega.privacy.android.domain.entity.transfer.pending

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Transfer nodes are fetched with different identifiers depending on the node type.
 */
@Serializable
sealed interface PendingTransferNodeIdentifier {

    /**
     * Identifier for ordinary Cloud Drive nodes
     * @property nodeId
     */
    @Serializable
    data class CloudDriveNode(val nodeId: NodeId) : PendingTransferNodeIdentifier

    /**
     * Chat attachment node identifier
     * @property chatId
     * @property messageId
     * @property messageIndex
     */
    @Serializable
    data class ChatAttachment(
        val chatId: Long,
        val messageId: Long,
        val messageIndex: Int,
    ) : PendingTransferNodeIdentifier

    /**
     * Public link file identifier
     * @property serializedData
     * @property nodeId
     */
    @Serializable
    data class PublicLinkFile(
        val serializedData: String?,
        val nodeId: NodeId,
    ) : PendingTransferNodeIdentifier

    /**
     * Public link folder identifier
     * @property nodeId
     */
    @Serializable
    data class PublicLinkFolder(val nodeId: NodeId) : PendingTransferNodeIdentifier

}