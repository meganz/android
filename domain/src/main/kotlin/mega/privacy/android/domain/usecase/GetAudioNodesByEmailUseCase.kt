package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting audio nodes by email
 */
class GetAudioNodesByEmailUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Getting audio nodes by email
     *
     * @param email email
     * @return audio nodes
     */
    suspend operator fun invoke(email: String) =
        mediaPlayerRepository.getAudioNodesByEmail(email)?.map { addNodeType(it) }
}