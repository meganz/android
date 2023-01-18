package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation of [GetVideoNodes]
 */
class DefaultGetVideoNodes @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetVideoNodes {
    override suspend fun invoke(order: SortOrder): List<TypedNode> =
        mediaPlayerRepository.getVideoNodes(order).map { addNodeType(it) }
}