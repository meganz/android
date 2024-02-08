package mega.privacy.android.data.repository.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.chat.ChatStorageGateway
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.chat.messages.PendingMessageEntityMapper
import mega.privacy.android.data.mapper.chat.messages.PendingMessageTypedMessageEntityMapper
import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

internal class ChatMessageRepositoryImpl @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    private val chatStorageGateway: ChatStorageGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringListMapper: StringListMapper,
    private val handleListMapper: HandleListMapper,
    private val chatMessageMapper: ChatMessageMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
    private val pendingMessageTypedMessageEntityMapper: PendingMessageTypedMessageEntityMapper,
    private val pendingMessageEntityMapper: PendingMessageEntityMapper,
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

    override suspend fun sendGiphy(
        chatId: Long,
        srcMp4: String?,
        srcWebp: String?,
        sizeMp4: Long,
        sizeWebp: Long,
        width: Int,
        height: Int,
        title: String?,
    ) = withContext(ioDispatcher) {
        chatMessageMapper(
            megaChatApiGateway.sendGiphy(
                chatId,
                srcMp4,
                srcWebp,
                sizeMp4,
                sizeWebp,
                width,
                height,
                title
            )
        )
    }

    override suspend fun attachContact(chatId: Long, contactEmail: String): ChatMessage? =
        withContext(ioDispatcher) {
            megaApiGateway.getContact(contactEmail)?.let { user ->
                megaHandleListMapper(listOf(user.handle))?.let {
                    chatMessageMapper(megaChatApiGateway.attachContacts(chatId, it))
                }
            }
        }

    override suspend fun savePendingMessage(savePendingMessageRequest: SavePendingMessageRequest): PendingMessage {
        return withContext(ioDispatcher) {
            val message = pendingMessageTypedMessageEntityMapper(savePendingMessageRequest)
            val pendingMessage = pendingMessageEntityMapper(savePendingMessageRequest)
            val id = chatStorageGateway.storePendingMessage(message, pendingMessage)

            PendingMessage(
                id = id,
                chatId = savePendingMessageRequest.chatId,
                type = savePendingMessageRequest.type,
                uploadTimestamp = savePendingMessageRequest.uploadTimestamp,
                state = savePendingMessageRequest.state.value,
                tempIdKarere = savePendingMessageRequest.tempIdKarere,
                videoDownSampled = savePendingMessageRequest.videoDownSampled,
                filePath = savePendingMessageRequest.filePath,
                nodeHandle = savePendingMessageRequest.nodeHandle,
                fingerprint = savePendingMessageRequest.fingerprint,
                name = savePendingMessageRequest.name,
                transferTag = savePendingMessageRequest.transferTag,
            )
        }
    }
}