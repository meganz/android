package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting root node
 */
class GetRootNodeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * Get root node
     *
     * @return root node
     */
    suspend operator fun invoke() = mediaPlayerRepository.getRootNode()
}