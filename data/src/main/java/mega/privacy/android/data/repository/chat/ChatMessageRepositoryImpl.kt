package mega.privacy.android.data.repository.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

internal class ChatMessageRepositoryImpl @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatMessageRepository {
    override suspend fun setMessageSeen(chatId: Long, messageId: Long) = withContext(ioDispatcher) {
        megaChatApiGateway.setMessageSeen(chatId, messageId)
    }

    override suspend fun getLastMessageSeenId(chatId: Long): Long = withContext(ioDispatcher) {
        megaChatApiGateway.getLastMessageSeenId(chatId)
    }
}