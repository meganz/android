package mega.privacy.android.data.repository.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

internal class ChatMessageRepositoryImpl @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringListMapper: StringListMapper,
    private val handleListMapper: HandleListMapper,
) : ChatMessageRepository {
    override suspend fun setMessageSeen(chatId: Long, messageId: Long) = withContext(ioDispatcher) {
        megaChatApiGateway.setMessageSeen(chatId, messageId)
    }

    override suspend fun getLastMessageSeenId(chatId: Long): Long = withContext(ioDispatcher) {
        megaChatApiGateway.getLastMessageSeenId(chatId)
    }

    override suspend fun addReaction(chatId: Long, msgId: Long, reaction: String) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("addReaction") {}
                megaChatApiGateway.addReaction(chatId, msgId, reaction, listener)
                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun deleteReaction(chatId: Long, msgId: Long, reaction: String) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("deleteReaction") {}
                megaChatApiGateway.delReaction(chatId, msgId, reaction, listener)
                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun getMessageReactions(chatId: Long, msgId: Long) =
        withContext(ioDispatcher) {
            stringListMapper(megaChatApiGateway.getMessageReactions(chatId, msgId))
        }

    override suspend fun getMessageReactionCount(chatId: Long, msgId: Long, reaction: String) =
        withContext(ioDispatcher) {
            megaChatApiGateway.getMessageReactionCount(chatId, msgId, reaction)
        }

    override suspend fun getReactionUsers(chatId: Long, msgId: Long, reaction: String) =
        withContext(ioDispatcher) {
            handleListMapper(megaChatApiGateway.getReactionUsers(chatId, msgId, reaction))
        }
}