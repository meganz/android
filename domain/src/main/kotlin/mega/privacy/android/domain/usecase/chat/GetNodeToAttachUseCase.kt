package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filenode.GetOwnNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetOrCreateMyChatsFilesFolderIdUseCase
import javax.inject.Inject

/**
 * Use case to get node to attach
 *
 * @property getOwnNodeUseCase [GetOwnNodeUseCase]
 * @property getOrCreateMyChatsFilesFolderIdUseCase [GetOrCreateMyChatsFilesFolderIdUseCase]
 * @property getNodeByIdUseCase [GetOwnNodeUseCase]
 * @property copyNodeUseCase [CopyNodeUseCase]
 */
class GetNodeToAttachUseCase @Inject constructor(
    private val getOwnNodeUseCase: GetOwnNodeUseCase,
    private val getOrCreateMyChatsFilesFolderIdUseCase: GetOrCreateMyChatsFilesFolderIdUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val copyNodeUseCase: CopyNodeUseCase,
) {

    /**
     * Invoke
     *
     * @param fileNode [TypedFileNode]
     * @return [TypedNode]
     */
    suspend operator fun invoke(fileNode: TypedFileNode) =
        getOwnNodeUseCase(fileNode) ?: run {
            val chatFolderNodeID = getOrCreateMyChatsFilesFolderIdUseCase()

            val copiedNodeHandle = copyNodeUseCase(
                nodeToCopy = fileNode.id,
                newNodeParent = chatFolderNodeID,
                newNodeName = fileNode.name
            )

            val newNode = getNodeByIdUseCase(copiedNodeHandle)
            if (newNode is TypedFileNode) {
                getOwnNodeUseCase(newNode)
            } else {
                null
            }
        }
}