package mega.privacy.android.domain.usecase.transfers.mapper

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import javax.inject.Inject

/**
 * Mapper to get the [PendingTransferNodeIdentifier] from a given [TypedNode] that identifies it
 */
class GetPendingTransferNodeIdentifierUseCase @Inject constructor() {
    /**
     * Invoke
     * @param typedNode the [TypedNode] to get its identifier
     */
    operator fun invoke(typedNode: TypedNode): PendingTransferNodeIdentifier =
        when (typedNode) {
            is ChatFile -> {
                PendingTransferNodeIdentifier.ChatAttachment(
                    typedNode.chatId,
                    typedNode.messageId,
                    typedNode.messageIndex
                )
            }

            is PublicLinkFile -> {
                PendingTransferNodeIdentifier.PublicLinkFile(
                    typedNode.node.serializedData,
                    typedNode.id
                )
            }

            is PublicLinkFolder -> {
                PendingTransferNodeIdentifier.PublicLinkFolder(typedNode.id)
            }

            is TypedFileNode, is TypedFolderNode -> {
                PendingTransferNodeIdentifier.CloudDriveNode(typedNode.id)
            }

            else -> throw IllegalStateException("Invalid type")
        }
}