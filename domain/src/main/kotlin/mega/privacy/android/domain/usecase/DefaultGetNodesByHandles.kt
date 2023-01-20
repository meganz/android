package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation of [GetNodesByHandles]
 */
class DefaultGetNodesByHandles @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetNodesByHandles {
    override suspend fun invoke(handles: List<Long>): List<TypedNode> =
        mediaPlayerRepository.getNodesByHandles(handles).map { addNodeType(it) }
}