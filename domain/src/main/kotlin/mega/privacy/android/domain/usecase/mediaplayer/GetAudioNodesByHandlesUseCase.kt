package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting audio nodes by handles
 */
class GetAudioNodesByHandlesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Get audio nodes by handles
     *
     * @param handles handle list
     * @return audio nodes
     */
    suspend operator fun invoke(handles: List<Long>) =
        mediaPlayerRepository.getAudioNodesByHandles(handles)
}