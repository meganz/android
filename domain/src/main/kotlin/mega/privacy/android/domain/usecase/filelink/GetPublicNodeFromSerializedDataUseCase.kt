package mega.privacy.android.domain.usecase.filelink

import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import javax.inject.Inject

/**
 * Get [PublicLinkNode] from its serialized data
 * This use case is used in legacy view models working with MegaNodes, new view models should work directly with TypedNodes and should not need this use-case
 */
class GetPublicNodeFromSerializedDataUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(serializedData: String): PublicLinkNode? =
        nodeRepository.getNodeFromSerializedData(serializedData)?.let {
            mapNodeToPublicLinkUseCase(it, null)
        }
}