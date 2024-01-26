package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import javax.inject.Inject

/**
 * Get child [PublicLinkNode] node from its id, this node should be a folder link's child
 */
class GetPublicChildNodeFromIdUseCase @Inject constructor(
    private val folderLinkRepository: FolderLinkRepository,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(nodeId: NodeId): PublicLinkNode? =
        folderLinkRepository.getChildNode(nodeId)?.let {
            mapNodeToPublicLinkUseCase(it, null)
        }
}