package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting inbox node
 */
class GetInboxNodeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * Get inbox node
     *
     * @return inbox node
     */
    suspend operator fun invoke() = mediaPlayerRepository.getInboxNode()
}