package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation of [GetVideoNodesByEmail]
 */
class DefaultGetVideoNodesByEmail @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetVideoNodesByEmail {
    override suspend fun invoke(email: String): List<TypedNode>? =
        mediaPlayerRepository.getVideoNodesByEmail(email)?.map { addNodeType(it) }
}