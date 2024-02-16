package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to monitor folder link nodes
 */
class MonitorFolderLinkNodesUseCase @Inject constructor(
    private val folderLinkRepository: FolderLinkRepository,
    private val nodeRepository: NodeRepository,
) {
    private val nodesCache: MutableMap<NodeId, ImageNode> = mutableMapOf()

    operator fun invoke(parentId: NodeId): Flow<List<ImageNode>> = flow {
        emit(populateNodes(parentId))
        emitAll(monitorNodes(parentId))
    }

    private suspend fun populateNodes(parentId: NodeId): List<ImageNode> {
        val imageNodes = folderLinkRepository.getFolderLinkImageNodes(parentId.longValue, null)

        nodesCache.clear()
        nodesCache.putAll(imageNodes.associateBy { it.id })

        return nodesCache.values.toList()
    }

    private suspend fun monitorNodes(parentId: NodeId): Flow<List<ImageNode>> {
        return nodeRepository.monitorNodeUpdates()
            .map { _ ->
                populateNodes(parentId)
            }
    }
}
