package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting rubbish node
 */
class GetRubbishNodeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * Get rubbish node
     *
     * @return rubbish node
     */
    suspend operator fun invoke() = mediaPlayerRepository.getRubbishNode()
}