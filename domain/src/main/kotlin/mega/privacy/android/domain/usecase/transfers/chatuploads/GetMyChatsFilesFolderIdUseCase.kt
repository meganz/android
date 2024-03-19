package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import javax.inject.Inject

/**
 * Get the current chats files folder id if already set or set and return default folder
 */
class GetMyChatsFilesFolderIdUseCase @Inject constructor(
    private val createFolderNodeUseCase: CreateFolderNodeUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val nodeRepository: NodeRepository,
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): NodeId {
        val nodeId = fileSystemRepository.getMyChatsFilesFolderId()
        return if (nodeId == null || nodeRepository.getNodeById(nodeId) == null) {
            val handle =
                fileSystemRepository.setMyChatFilesFolder(
                    createFolderNodeUseCase(chatRepository.getDefaultChatFolderName()).longValue
                ) ?: throw Exception("Failed to set chat upload folder")
            NodeId(handle)
        } else {
            nodeId
        }
    }
}
