package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filenode.GetOwnNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import javax.inject.Inject

/**
 * Use case to get node to attach
 *
 * @property getOwnNodeUseCase [GetOwnNodeUseCase]
 * @property getMyChatsFilesFolderIdUseCase [GetMyChatsFilesFolderIdUseCase]
 * @property getNodeByIdUseCase [GetOwnNodeUseCase]
 * @property copyNodeUseCase [CopyNodeUseCase]
 */
class GetNodeToAttachUseCase @Inject constructor(
    private val getOwnNodeUseCase: GetOwnNodeUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
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
            val chatFolderNodeID = getMyChatsFilesFolderIdUseCase()

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