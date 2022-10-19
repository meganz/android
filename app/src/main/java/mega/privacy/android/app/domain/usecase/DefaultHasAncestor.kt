package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.HasAncestor
import javax.inject.Inject

class DefaultHasAncestor @Inject constructor(
    private val filesRepository: FilesRepository,
) : HasAncestor {

    override suspend fun invoke(targetNode: NodeId, ancestorId: NodeId): Boolean {
        val megaNode = filesRepository.getNodeByHandle(targetNode.id) ?: return false
        val currentId = NodeId(megaNode.handle)
        val parentId = NodeId(megaNode.parentHandle)
        return currentId == ancestorId || invoke(parentId, ancestorId)
    }
}