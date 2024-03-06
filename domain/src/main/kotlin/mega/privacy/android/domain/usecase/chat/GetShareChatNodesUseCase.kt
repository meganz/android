package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.NodeShareContentUri
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import javax.inject.Inject

/**
 * Get Share Chat Nodes Use Case
 *
 */
class GetShareChatNodesUseCase @Inject constructor(
    private val exportChatNodesUseCase: ExportChatNodesUseCase,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(chatNodes: List<ChatFile>): NodeShareContentUri {
        val files = chatNodes.mapNotNull {
            getNodePreviewFileUseCase(it)
        }
        return if (files.size == chatNodes.size) {
            NodeShareContentUri.LocalContentUris(files)
        } else {
            val links = exportChatNodesUseCase(chatNodes).map { (_, link) -> link }
            if (links.isEmpty()) throw IllegalStateException("No links to share")
            NodeShareContentUri.RemoteContentUris(links)
        }
    }
}