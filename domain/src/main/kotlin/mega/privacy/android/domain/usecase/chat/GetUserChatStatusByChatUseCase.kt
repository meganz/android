package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import javax.inject.Inject

/**
 * Get user chat status from chat id use case.
 */
class GetUserChatStatusByChatUseCase @Inject constructor(
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
) {
    /**
     * Invoke
     *
     * @param chat Chat identifiers.
     */
    suspend operator fun invoke(chat: ChatRoom) =
        if (chat.isGroup) null
        else getUserOnlineStatusByHandleUseCase(chat.peerHandlesList[0])
}
