package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.StreamingServerRepository
import javax.inject.Inject

/**
 * The use case for getting a URL to a node in the local HTTP proxy server from MegaApi
 */
class GetLocalLinkFromMegaApiUseCase @Inject constructor(
    private val streamingServerRepository: StreamingServerRepository,
) {

    /**
     * Get a URL to a node in the local HTTP proxy server from MegaApi
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend operator fun invoke(nodeHandle: Long) =
        streamingServerRepository.getFileStreamingUri(nodeHandle)
}