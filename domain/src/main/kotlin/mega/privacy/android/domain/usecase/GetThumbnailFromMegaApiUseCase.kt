package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting thumbnail from MegaApi
 */
class GetThumbnailFromMegaApiUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Get thumbnail from MegaApi
     *
     * @param nodeHandle node handle
     * @param path thumbnail path
     */
    suspend operator fun invoke(nodeHandle: Long, path: String) =
        mediaPlayerRepository.getThumbnailFromMegaApi(nodeHandle = nodeHandle, path = path)
}