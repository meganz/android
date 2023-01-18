package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation of [GetAudioNodesFromInShares]
 */
class DefaultGetAudioNodesFromInShares @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetAudioNodesFromInShares {
    override suspend fun invoke(order: SortOrder): List<TypedNode> =
        mediaPlayerRepository.getAudioNodesFromInShares(order).map { addNodeType(it) }
}