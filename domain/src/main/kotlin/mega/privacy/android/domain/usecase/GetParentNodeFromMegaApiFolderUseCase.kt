package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting parent node by handle from MegaApiFolder
 */
class GetParentNodeFromMegaApiFolderUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Get parent node by handle from MegaApiFolder
     *
     * @param parentHandle node handle
     * @return parent node
     */
    suspend operator fun invoke(parentHandle: Long) =
        mediaPlayerRepository.getParentNodeFromMegaApiFolder(parentHandle)
}