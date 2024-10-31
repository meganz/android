package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import javax.inject.Inject

/**
 * Use case to get the [TypedNode] of a given a [PendingTransfer]
 */
class GetTypedNodeFromPendingTransferUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase,
    private val nodeRepository: NodeRepository,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
) {
    /**
     * Invoke
     * @param pendingTransfer
     */
    suspend operator fun invoke(pendingTransfer: PendingTransfer): TypedNode? =
        pendingTransfer.nodeIdentifier.let { id ->
            when (id) {
                is PendingTransferNodeIdentifier.CloudDriveNode -> getNodeByIdUseCase(id.nodeId)
                is PendingTransferNodeIdentifier.ChatAttachment -> getChatFileUseCase(
                    id.chatId,
                    id.messageId,
                    id.messageIndex
                )

                is PendingTransferNodeIdentifier.PublicLinkFile -> {
                    id.serializedData?.let { getPublicNodeFromSerializedDataUseCase(it) }
                        ?: getPublicLinkNode(id.nodeId)
                }

                is PendingTransferNodeIdentifier.PublicLinkFolder -> getPublicLinkNode(id.nodeId)
            }
        }

    private suspend fun getPublicLinkNode(nodeId: NodeId): PublicLinkNode? =
        nodeRepository.getNodeByHandle(nodeId.longValue, attemptFromFolderApi = true)?.let {
            mapNodeToPublicLinkUseCase(it as UnTypedNode, null)
        }
}