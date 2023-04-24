package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting a URL to a node in the local HTTP proxy server from MegaApi
 */
class GetLocalLinkFromMegaApiUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Get a URL to a node in the local HTTP proxy server from MegaApi
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend operator fun invoke(nodeHandle: Long) =
        mediaPlayerRepository.getLocalLinkFromMegaApi(nodeHandle)
}