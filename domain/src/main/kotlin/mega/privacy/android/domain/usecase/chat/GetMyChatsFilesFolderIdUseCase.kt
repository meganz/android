package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Get the current chats files folder id if exists and is not in the rubbish been or deleted.
 */
class GetMyChatsFilesFolderIdUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): NodeId? =
        chatRepository.getMyChatsFilesFolderId()
            ?.takeIf { !isNodeInRubbishOrDeletedUseCase(it.longValue) }
}
