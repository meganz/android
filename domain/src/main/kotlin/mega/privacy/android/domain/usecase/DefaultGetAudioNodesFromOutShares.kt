package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation for getting audio nodes from out shares
 */
class DefaultGetAudioNodesFromOutShares @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetAudioNodesFromOutShares {
    override suspend fun invoke(lastHandle: Long, order: SortOrder): List<TypedNode> =
        mediaPlayerRepository.getAudioNodesFromOutShares(lastHandle, order).map { addNodeType(it) }
}