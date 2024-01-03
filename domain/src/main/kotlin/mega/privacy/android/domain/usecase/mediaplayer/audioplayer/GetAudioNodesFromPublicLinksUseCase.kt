package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for get audio nodes from public links
 */
class GetAudioNodesFromPublicLinksUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Get audio nodes from public links
     *
     * @param order [SortOrder]
     * @return audio nodes
     */

    suspend operator fun invoke(order: SortOrder): List<TypedAudioNode> =
        mediaPlayerRepository.getAudioNodesFromPublicLinks(order)
}