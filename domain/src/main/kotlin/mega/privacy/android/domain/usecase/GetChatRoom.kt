package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * Use case for getting the updated main data of a chat room.
 */
fun interface GetChatRoom {

    /**
     * Invoke.
     *
     * @param chatId        Chat id.
     * @return [ChatRoom]   containing the updated data.
     */
    operator fun invoke(chatId: Long): ChatRoom?
}