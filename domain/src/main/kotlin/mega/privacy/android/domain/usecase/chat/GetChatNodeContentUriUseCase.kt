package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNode
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import javax.inject.Inject

/**
 * Get Chat Node Content Uri Use Case
 *
 */
class GetChatNodeContentUriUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getLocalFileForNode: GetLocalFileForNode,
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(
        fileNode: ChatFile,
    ): NodeContentUri = getLocalFileForNode(fileNode)?.let {
        NodeContentUri.LocalContentUri(it)
    } ?: run {
        val shouldStopHttpSever = if (httpServerIsRunning() == 0) {
            httpServerStart()
            true
        } else false
        val link = nodeRepository.getLocalLink(fileNode)
        NodeContentUri.RemoteContentUri(link, shouldStopHttpSever)
    }
}