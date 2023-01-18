package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation for getting video nodes from out shares
 */
class DefaultGetVideoNodesFromOutShares @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetVideoNodesFromOutShares {
    override suspend fun invoke(lastHandle: Long, order: SortOrder): List<TypedNode> =
        mediaPlayerRepository.getVideoNodesFromOutShares(lastHandle, order).map { addNodeType(it) }
}