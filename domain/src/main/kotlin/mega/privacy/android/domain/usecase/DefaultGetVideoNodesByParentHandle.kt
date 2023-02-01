package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation of [GetVideoNodesByParentHandle]
 */
class DefaultGetVideoNodesByParentHandle @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetVideoNodesByParentHandle {
    override suspend fun invoke(parentHandle: Long, order: SortOrder): List<TypedNode>? =
        mediaPlayerRepository.getVideoNodesByParentHandle(parentHandle, order)?.map {
            addNodeType(it)
        }
}