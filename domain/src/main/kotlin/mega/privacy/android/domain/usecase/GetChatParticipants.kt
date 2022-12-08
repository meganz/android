package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatParticipant

/**
 * Get Chat participants use case.
 */
fun interface GetChatParticipants {
    /**
     * Get the participants of a chat.
     *
     * @param chatId    Chat Id.
     * @return a flow list of [ChatParticipant].
     */
    operator fun invoke(chatId: Long): Flow<List<ChatParticipant>>

}