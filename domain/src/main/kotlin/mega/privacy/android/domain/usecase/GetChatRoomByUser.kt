package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * Use case for getting the updated main data of a chat room from user info
 */
fun interface GetChatRoomByUser {
    /**
     * Invoke.
     *
     * @param userHandle        User handle.
     * @return [ChatRoom]   containing the updated data.
     */
    suspend operator fun invoke(userHandle: Long): ChatRoom?
}