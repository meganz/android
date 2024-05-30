package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.ResourceAlreadyExistsMegaException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Get the current chats files folder id if already set or set and return default folder.
 */
class GetOrCreateMyChatsFilesFolderIdUseCase @Inject constructor(
    private val createFolderNodeUseCase: CreateFolderNodeUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val chatRepository: ChatRepository,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): NodeId {
        val nodeId = fileSystemRepository.getMyChatsFilesFolderId()
        return if (nodeId == null || isNodeInRubbishOrDeletedUseCase(nodeId.longValue)) {
            val defaultChatFolderName = chatRepository.getDefaultChatFolderName()
            val chatFolderNodeId = runCatching {
                createFolderNodeUseCase(defaultChatFolderName).longValue
            }.getOrElse { throwable ->
                if (throwable is ResourceAlreadyExistsMegaException) {
                    getExistedFolderHandle(defaultChatFolderName) ?: throw throwable
                } else {
                    throw throwable
                }
            }
            val handle =
                fileSystemRepository.setMyChatFilesFolder(chatFolderNodeId)
                    ?: throw Exception("Failed to set chat upload folder")
            NodeId(handle)
        } else {
            nodeId
        }
    }

    private suspend fun getExistedFolderHandle(
        defaultChatFolderName: String,
    ) = getRootNodeUseCase()?.let { rootNode ->
        getChildNodeUseCase(rootNode.id, defaultChatFolderName)
    }?.id?.longValue
}
