package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting video nodes by email
 */
class GetVideoNodesByEmailUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Getting video nodes by email
     *
     * @param email email
     * @return video nodes
     */
    suspend operator fun invoke(email: String) =
        mediaPlayerRepository.getVideoNodesByEmail(email)?.map { addNodeType(it) }
}