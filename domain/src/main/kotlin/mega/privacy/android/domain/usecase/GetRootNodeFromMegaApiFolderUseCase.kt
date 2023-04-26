package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting root node from MegaApiFolder
 */
class GetRootNodeFromMegaApiFolderUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * Get root node from MegaApiFolder
     *
     * @return root node
     */
    suspend operator fun invoke() = mediaPlayerRepository.getRootNodeFromMegaApiFolder()
}