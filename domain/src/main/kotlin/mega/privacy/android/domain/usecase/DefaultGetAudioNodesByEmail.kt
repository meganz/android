package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation of [GetAudioNodesByEmail]
 */
class DefaultGetAudioNodesByEmail @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetAudioNodesByEmail {
    override suspend fun invoke(email: String): List<TypedNode>? =
        mediaPlayerRepository.getAudioNodesByEmail(email)?.map { addNodeType(it) }
}