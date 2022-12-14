package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.ChatCall

/**
 * Use case for getting the updated main data of a chat room.
 */
fun interface GetChatCall {

    /**
     * Invoke.
     *
     * @param chatId    Chat id.
     * @return          [ChatCall]
     */
    suspend operator fun invoke(chatId: Long): ChatCall?
}