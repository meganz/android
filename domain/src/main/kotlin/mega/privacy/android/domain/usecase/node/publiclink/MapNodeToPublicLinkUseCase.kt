package mega.privacy.android.domain.usecase.node.publiclink

import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Map node to public link use case
 *
 * @property addNodeType
 * @property mapTypedNodeToPublicLinkUseCase
 */
class MapNodeToPublicLinkUseCase @Inject constructor(
    private val addNodeType: AddNodeType,
    private val mapTypedNodeToPublicLinkUseCase: MapTypedNodeToPublicLinkUseCase,
) {
    /**
     * Invoke
     *
     * @param node
     * @return [PublicLinkNode]
     */
    suspend operator fun invoke(node: UnTypedNode): PublicLinkNode =
        mapTypedNodeToPublicLinkUseCase(addNodeType(node))
}
