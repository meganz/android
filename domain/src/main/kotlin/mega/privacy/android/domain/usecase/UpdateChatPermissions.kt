package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Use case for update chat permissions
 */
fun interface UpdateChatPermissions {

    /**
     * Invoke.
     *
     * @param chatId        The chat id.
     * @param handle        User handle.
     * @param permission    User privilege.
     * @return              The Chat Request.
     */
    suspend operator fun invoke(
        chatId: Long,
        handle: Long,
        permission: ChatRoomPermission
    ): ChatRequest
}