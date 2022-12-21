package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FileRepository
import javax.inject.Inject

/**
 * Default has ancestor
 *
 * @property fileRepository
 */
class DefaultHasAncestor @Inject constructor(
    private val fileRepository: FileRepository,
) : HasAncestor {

    override suspend fun invoke(targetNodeId: NodeId, ancestorId: NodeId): Boolean {
        val node = fileRepository.getNodeById(targetNodeId) ?: return false
        val currentId = node.id
        val parentId = node.parentId
        return currentId == ancestorId || invoke(parentId, ancestorId)
    }
}