package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNode
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.chat.AddChatFileTypeUseCase
import javax.inject.Inject

/**
 * Get Chat Node Content Uri Use Case
 *
 */
class GetChatNodeContentUriUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addChatFileTypeUseCase: AddChatFileTypeUseCase,
    private val getLocalFileForNode: GetLocalFileForNode,
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(
        chatId: Long,
        msgId: Long,
        fileNode: FileNode,
    ): NodeContentUri = getLocalFileForNode(fileNode)?.let {
        NodeContentUri.LocalContentUri(it)
    } ?: run {
        val shouldStopHttpSever = if (httpServerIsRunning() == 0) {
            httpServerStart()
            true
        } else false
        val typedNode = addChatFileTypeUseCase(fileNode, chatId, msgId)
        val link = nodeRepository.getLocalLink(typedNode)
        NodeContentUri.RemoteContentUri(link, shouldStopHttpSever)
    }
}