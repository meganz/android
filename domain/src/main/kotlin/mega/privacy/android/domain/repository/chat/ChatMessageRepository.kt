package mega.privacy.android.domain.repository.chat

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Chat message repository
 *
 */
interface ChatMessageRepository {
    /**
     * Set message seen
     *
     * @param chatId Chat id
     * @param messageId Message id
     */
    suspend fun setMessageSeen(chatId: Long, messageId: Long): Boolean

    /**
     * Get last message seen id
     *
     * @param chatId Chat id
     * @return Last message seen id
     */
    suspend fun getLastMessageSeenId(chatId: Long): Long

    /**
     * Adds a reaction for a message in a chatroom
     *
     * The reactions updates will be notified one by one through the MegaChatRoomListener
     * specified at MegaChatApi::openChatRoom (and through any other listener you may have
     * registered by calling MegaChatApi::addChatRoomListener). The corresponding callback
     * is MegaChatRoomListener::onReactionUpdate.
     *
     * Note that receiving an onRequestFinish with the error code MegaChatError::ERROR_OK, does not ensure
     * that add reaction has been applied in chatd. As we've mentioned above, reactions updates will
     * be notified through callback MegaChatRoomListener::onReactionUpdate.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_MANAGE_REACTION
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chatid that identifies the chatroom
     * - MegaChatRequest::getUserHandle - Returns the msgid that identifies the message
     * - MegaChatRequest::getText - Returns a UTF-8 NULL-terminated string that represents the reaction
     * - MegaChatRequest::getFlag - Returns true indicating that requested action is add reaction
     *
     * On the onRequestFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ARGS - if reaction is NULL or the msgid references a management message.
     * - MegaChatError::ERROR_NOENT - if the chatroom/message doesn't exists
     * - MegaChatError::ERROR_ACCESS - if our own privilege is different than MegaChatPeerList::PRIV_STANDARD
     * or MegaChatPeerList::PRIV_MODERATOR.
     * - MegaChatError::ERROR_EXIST - if our own user has reacted previously with this reaction for this message
     *
     * @param chatId MegaChatHandle that identifies the chatroom
     * @param msgId MegaChatHandle that identifies the message
     * @param reaction UTF-8 NULL-terminated string that represents the reaction
     */
    suspend fun addReaction(chatId: Long, msgId: Long, reaction: String)

    /**
     * Removes a reaction for a message in a chatroom
     *
     * The reactions updates will be notified one by one through the MegaChatRoomListener
     * specified at MegaChatApi::openChatRoom (and through any other listener you may have
     * registered by calling MegaChatApi::addChatRoomListener). The corresponding callback
     * is MegaChatRoomListener::onReactionUpdate.
     *
     * Note that receiving an onRequestFinish with the error code MegaChatError::ERROR_OK, does not ensure
     * that remove reaction has been applied in chatd. As we've mentioned above, reactions updates will
     * be notified through callback MegaChatRoomListener::onReactionUpdate.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_MANAGE_REACTION
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chatid that identifies the chatroom
     * - MegaChatRequest::getUserHandle - Returns the msgid that identifies the message
     * - MegaChatRequest::getText - Returns a UTF-8 NULL-terminated string that represents the reaction
     * - MegaChatRequest::getFlag - Returns false indicating that requested action is remove reaction
     *
     * On the onRequestFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ARGS: if reaction is NULL or the msgid references a management message.
     * - MegaChatError::ERROR_NOENT: if the chatroom/message doesn't exists
     * - MegaChatError::ERROR_ACCESS: if our own privilege is different than MegaChatPeerList::PRIV_STANDARD
     * or MegaChatPeerList::PRIV_MODERATOR
     * - MegaChatError::ERROR_EXIST - if your own user has not reacted to the message with the specified reaction.
     *
     * @param chatId MegaChatHandle that identifies the chatroom
     * @param msgId MegaChatHandle that identifies the message
     * @param reaction UTF-8 NULL-terminated string that represents the reaction
     */
    suspend fun deleteReaction(chatId: Long, msgId: Long, reaction: String)

    /**
     * Gets a list of reactions associated to a message
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chatroom
     * @param msgId MegaChatHandle that identifies the message
     * @return return a list with the reactions associated to a message.
     */
    suspend fun getMessageReactions(chatId: Long, msgId: Long): List<String>

    /**
     * Returns the number of users that reacted to a message with a specific reaction
     *
     * @param chatId MegaChatHandle that identifies the chatroom
     * @param msgId MegaChatHandle that identifies the message
     * @param reaction UTF-8 NULL terminated string that represents the reactiongaC
     *
     * @return return the number of users that reacted to a message with a specific reaction,
     * or -1 if the chatroom or message is not found.
     */
    suspend fun getMessageReactionCount(chatId: Long, msgId: Long, reaction: String): Int

    /**
     * Gets a list of users that reacted to a message with a specific reaction
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chatroom
     * @param msgId MegaChatHandle that identifies the message
     * @param reaction UTF-8 NULL terminated string that represents the reaction
     *
     * @return return a list with the users that reacted to a message with a specific reaction.
     */
    suspend fun getReactionUsers(chatId: Long, msgId: Long, reaction: String): List<Long>

    /**
     * Sends a new giphy to the specified chatroom
     *
     * The MegaChatMessage object returned by this function includes a message transaction id,
     * That id is not the definitive id, which will be assigned by the server. You can obtain the
     * temporal id with MegaChatMessage::getTempId
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to MEGACHAT_INVALID_HANDLE.
     *
     * You take the ownership of the returned value.
     *
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param srcMp4 Source location of the mp4
     * @param srcWebp Source location of the webp
     * @param sizeMp4 Size in bytes of the mp4
     * @param sizeWebp Size in bytes of the webp
     * @param width Width of the giphy
     * @param height Height of the giphy
     * @param title Title of the giphy
     *
     * @return ChatMessage that will be sent. The message id is not definitive, but temporal.
     */
    suspend fun sendGiphy(
        chatId: Long,
        srcMp4: String?,
        srcWebp: String?,
        sizeMp4: Long,
        sizeWebp: Long,
        width: Int,
        height: Int,
        title: String?,
    ): ChatMessage

    /**
     * Sends a contact to the specified chatroom
     *
     * The MegaChatMessage object returned by this function includes a message transaction id,
     * That id is not the definitive id, which will be assigned by the server. You can obtain the
     * temporal id with MegaChatMessage::getTempId()
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to MEGACHAT_INVALID_HANDLE.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param contactEmail Contact email
     * @return ChatMessage that will be sent. The message id is not definitive, but temporal.
     */
    suspend fun attachContact(chatId: Long, contactEmail: String): ChatMessage?

    /**
     * Save pending message
     *
     * @param savePendingMessageRequest
     * @return saved PendingMessage
     */
    suspend fun savePendingMessage(savePendingMessageRequest: SavePendingMessageRequest): PendingMessage

    /**
     * Monitor pending messages
     *
     * @param chatId
     * @return flow of pending messages for the chat
     */
    fun monitorPendingMessages(chatId: Long): Flow<List<PendingMessage>>

    /**
     * Forward a message with attach contact
     *
     * The MegaChatMessage object returned by this function includes a message transaction id,
     * That id is not the definitive id, which will be assigned by the server. You can obtain the
     * temporal id with MegaChatMessage::getTempId()
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to MEGACHAT_INVALID_HANDLE.
     *
     * You take the ownership of the returned value.
     *
     * @param sourceChatId MegaChatHandle that identifies the chat room where the source message is
     * @param msgId MegaChatHandle that identifies the message that is going to be forwarded
     * @param targetChatId MegaChatHandle that identifies the chat room where the message is going to be forwarded
     * @return ChatMessage that will be sent. The message id is not definitive, but temporal.
     */
    suspend fun forwardContact(sourceChatId: Long, msgId: Long, targetChatId: Long): ChatMessage?

    /**
     * Sends a node to the specified chatroom
     *
     * The attachment message includes information about the node, so the receiver can download
     * or import the node.
     *
     * In contrast to other functions to send messages, such as
     * MegaChatApi::sendMessage or MegaChatApi::attachContacts, this function
     * is asynchronous and does not return a MegaChatMessage directly. Instead, the
     * MegaChatMessage can be obtained as a result of the corresponding MegaChatRequest.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_ATTACH_NODE_MESSAGE
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns the handle of the node
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     * - MegaChatRequest::getMegaChatMessage - Returns the message that has been sent
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to MEGACHAT_INVALID_HANDLE.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param nodeHandle Handle of the node that the user wants to attach
     * @return Msg id.
     */
    suspend fun attachNode(chatId: Long, nodeHandle: Long): Long?

    /**
     * Sends a node that contains a voice message to the specified chatroom
     *
     * The voice clip message includes information about the node, so the receiver can reproduce it online.
     *
     * In contrast to other functions to send messages, such as MegaChatApi::sendMessage or
     * MegaChatApi::attachContacts, this function is asynchronous and does not return a MegaChatMessage
     * directly. Instead, the MegaChatMessage can be obtained as a result of the corresponding MegaChatRequest.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_ATTACH_NODE_MESSAGE
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns the handle of the node
     * - MegaChatRequest::getParamType - Returns 1 (to identify the attachment as a voice message)
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     * - MegaChatRequest::getMegaChatMessage - Returns the message that has been sent
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to MEGACHAT_INVALID_HANDLE.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param nodeHandle Handle of the node that the user wants to attach
     * @return Identifier of the temp message attached.
     */
    suspend fun attachVoiceMessage(chatId: Long, nodeHandle: Long): Long?

    /**
     * Fetch pending messages by id
     *
     * @param pendingMessageId
     * @return a pending messages with [pendingMessageId] or null if not found
     */
    suspend fun getPendingMessage(pendingMessageId: Long): PendingMessage?

    /**
     * Delete pending message by id
     *
     * @param pendingMessage
     */
    suspend fun deletePendingMessage(pendingMessage: PendingMessage)

    /**
     * Get message ids by type
     *
     * @param chatId
     * @param type
     * @return list of message ids
     */
    suspend fun getMessageIdsByType(chatId: Long, type: ChatMessageType): List<Long>

    /**
     * Get message
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @return Message reactions
     */
    suspend fun getReactionsFromMessage(chatId: Long, msgId: Long): List<Reaction>

    /**
     * Update message reactions.
     *
     * @param chatId Chat ID
     * @param msgId Message ID
     * @param reactions Updated reactions
     */
    suspend fun updateReactionsInMessage(chatId: Long, msgId: Long, reactions: List<Reaction>)
}