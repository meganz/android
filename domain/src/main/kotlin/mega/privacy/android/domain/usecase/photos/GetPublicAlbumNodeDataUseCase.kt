package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Get public album node data use case
 */
class GetPublicAlbumNodeDataUseCase @Inject constructor(
    private val getPublicAlbumNodesDataUseCase: GetPublicAlbumNodesDataUseCase,
) {
    operator fun invoke(nodeId: NodeId): String? {
        return getPublicAlbumNodesDataUseCase()[nodeId]
    }
}
