package mega.privacy.android.domain.usecase

/**
 * The use case for getting a URL to a node in the local HTTP proxy server from MegaApi
 */
fun interface GetLocalLinkFromMegaApi {

    /**
     * Get a URL to a node in the local HTTP proxy server from MegaApi
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend operator fun invoke(nodeHandle: Long): String?
}