package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.usecase.filelink.GetFileUrlByPublicLinkUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import javax.inject.Inject

/**
 * The Use case is used to get the node content uri of a file link
 */
class GetFileLinkNodeContentUriUseCase @Inject constructor(
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val getFileUrlByPublicLinkUseCase: GetFileUrlByPublicLinkUseCase,
) {
    /**
     * Invoke
     *
     * @param link file link
     * @return [NodeContentUri]
     */
    suspend operator fun invoke(link: String): NodeContentUri {
        val shouldStopHttpSever = if (httpServerIsRunning() == 0) {
            httpServerStart()
            true
        } else false
        val url =
            getFileUrlByPublicLinkUseCase(link) ?: throw IllegalStateException("Local link is null")
        return NodeContentUri.RemoteContentUri(url, shouldStopHttpSever)
    }
}