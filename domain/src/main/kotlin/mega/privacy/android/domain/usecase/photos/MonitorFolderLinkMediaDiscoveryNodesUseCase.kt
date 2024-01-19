package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case to monitor folder link media discovery nodes
 */
class MonitorFolderLinkMediaDiscoveryNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    private val nodesCache: MutableMap<NodeId, ImageNode> = mutableMapOf()

    operator fun invoke(parentId: NodeId, recursive: Boolean): Flow<List<ImageNode>> = flow {
        emit(populateNodes(parentId, recursive))
    }

    private suspend fun populateNodes(parentId: NodeId, recursive: Boolean): List<ImageNode> {
        val nodes = photosRepository.getPublicMediaDiscoveryNodes(
            parentId = parentId,
            recursive = recursive,
        )

        nodesCache.clear()
        nodesCache.putAll(nodes.associateBy { it.id })

        return nodesCache.values.toList()
    }
}
