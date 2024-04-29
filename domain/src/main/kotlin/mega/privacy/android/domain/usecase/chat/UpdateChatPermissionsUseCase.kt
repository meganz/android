package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for update chat permissions
 *
 * @property chatRepository [ChatRepository]
 */
class UpdateChatPermissionsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    /**
     * Invoke.
     *
     * @param chatId        The chat id.
     * @param nodeId        User handle.
     * @param permission    User privilege.
     * @return              The Chat Request.
     */
    suspend operator fun invoke(
        chatId: Long,
        nodeId: NodeId,
        permission: ChatRoomPermission
    ): ChatRequest = chatRepository.updateChatPermissions(
        chatId = chatId,
        nodeId = nodeId,
        permission = permission
    )
}
