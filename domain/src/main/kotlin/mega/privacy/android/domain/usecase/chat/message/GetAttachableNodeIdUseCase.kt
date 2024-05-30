package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.CopyTypedNodeUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetOrCreateMyChatsFilesFolderIdUseCase
import javax.inject.Inject

/**
 * Get a NodeId that can be used to attach the node to a chat.
 * If node is in the current logged user account its id will be directly returned
 * If not, the node will be copied to chat files folder and the resulting id returned
 */
class GetAttachableNodeIdUseCase @Inject constructor(
    private val copyNodeUseCase: CopyTypedNodeUseCase,
    private val getOrCreateMyChatsFilesFolderIdUseCase: GetOrCreateMyChatsFilesFolderIdUseCase,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(fileNode: TypedFileNode): NodeId {
        val userHandle = nodeRepository.getMyUserHandleBinary()
        return if (userHandle == nodeRepository.getOwnerNodeHandle(fileNode.id)) {
            fileNode.id
        } else {
            copyNodeUseCase(fileNode, getOrCreateMyChatsFilesFolderIdUseCase(), null)
        }
    }
}