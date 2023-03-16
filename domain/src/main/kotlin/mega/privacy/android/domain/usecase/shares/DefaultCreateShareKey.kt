package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default implementation for [CreateShareKey]
 */
class DefaultCreateShareKey @Inject constructor(private val nodeRepository: NodeRepository) :
    CreateShareKey {
    override suspend fun invoke(node: TypedFolderNode) {
        nodeRepository.createShareKey(node)
    }
}