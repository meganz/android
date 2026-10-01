package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import javax.inject.Inject

/**
 * Get Node Content Uri Use Case
 *
 */
class GetNodeContentUriUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(
        fileNode: TypedFileNode,
    ): NodeContentUri = getNodePreviewFileUseCase(fileNode)?.let {
        NodeContentUri.LocalContentUri(it)
    } ?: run {
        val shouldStopHttpSever = if (httpServerIsRunning() == 0) {
            httpServerStart()
            true
        } else false
        val link = nodeRepository.getLocalLink(fileNode)
            ?: throw IllegalStateException("Local link is null")
        NodeContentUri.RemoteContentUri(link, shouldStopHttpSever)
    }
}