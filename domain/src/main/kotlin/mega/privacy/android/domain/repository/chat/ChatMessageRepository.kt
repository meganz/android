package mega.privacy.android.domain.repository.chat

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.UserMessage
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.NodeId

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
     * Save multiple pending messages
     *
     * @param savePendingMessageRequest
     * @param chatIds List of chat ids
     * @return list of resulting pending messages ids
     */
    suspend fun savePendingMessages(
        savePendingMessageRequest: SavePendingMessageRequest,
        chatIds: List<Long>,
    ): List<Long>

    /**
     * Update pending message
     *
     * @param updatePendingMessageRequests
     */
    suspend fun updatePendingMessage(
        vararg updatePendingMessageRequests: UpdatePendingMessageRequest,
    )

    /**
     * Monitor pending messages for a chat
     *
     * @param chatId
     * @return flow of pending messages for the chat
     */
    fun monitorPendingMessages(chatId: Long): Flow<List<PendingMessage>>

    /**
     * Monitor pending messages of a specific state
     *
     * @param states
     * @return flow of pending messages for specific state
     */
    fun monitorPendingMessagesByState(vararg states: PendingMessageState): Flow<List<PendingMessage>>

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
     * @param nodeId node id of the node that the user wants to attach
     * @return Msg id.
     */
    suspend fun attachNode(chatId: Long, nodeId: NodeId): Long?

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
     * Get all pending messages in a specific state
     */
    suspend fun getPendingMessagesByState(state: PendingMessageState): List<PendingMessage>

    /**
     * Delete pending message
     *
     * @param pendingMessage
     */
    suspend fun deletePendingMessage(pendingMessage: PendingMessage)

    /**
     * Delete pending message by id
     *
     * @param pendingMessageId
     */
    suspend fun deletePendingMessageById(pendingMessageId: Long)

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

    /**
     * Deletes an existing message
     *
     * Message's deletions are equivalent to message's edits, but with empty content.
     * @see \c MegaChatapi::editMessage for more information.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     *
     * @return ChatMessage that will be deleted. NULL if the message cannot be deleted (too old)
     */
    suspend fun deleteMessage(chatId: Long, msgId: Long): ChatMessage?

    /**
     * Revoke the access to a node granted by an attachment message
     *
     * The attachment message will be deleted as any other message. Therefore,
     *
     * The revoke is actually a deletion of the former message. Hence, the behavior is the
     * same than a regular deletion.
     * @see MegaChatApi::editMessage or MegaChatApi::deleteMessage for more information.
     *
     * If the revoke is rejected because the attachment message is too old, or if the message is
     * not an attachment message, this function returns NULL.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     *
     * @return ChatMessage that will be modified. NULL if the message cannot be edited (too old)
     */
    suspend fun revokeAttachmentMessage(chatId: Long, msgId: Long): ChatMessage?


    /**
     * Edits an existing message
     *
     * Message's edits are only allowed during a short timeframe, usually 1 hour.
     * Message's deletions are equivalent to message's edits, but with empty content.
     *
     * There is only one pending edit for not-yet confirmed edits. Therefore, this function will
     * discard previous edits that haven't been notified via MegaChatRoomListener::onMessageUpdate
     * where the message has MegaChatMessage::hasChanged(MegaChatMessage::CHANGE_TYPE_CONTENT).
     *
     * If the edits is rejected... // TODO:
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     * @param msg New content of the message
     *
     * @return ChatMessage that will be modified. NULL if the message cannot be edited (too old)
     */
    suspend fun editMessage(chatId: Long, msgId: Long, msg: String): ChatMessage?

    /**
     * Edit a geolocation message
     *
     * Message's edits are only allowed during a short timeframe, usually 1 hour.
     * Message's deletions are equivalent to message's edits, but with empty content.
     *
     * There is only one pending edit for not-yet confirmed edits. Therefore, this function will
     * discard previous edits that haven't been notified via MegaChatRoomListener::onMessageUpdate
     * where the message has MegaChatMessage::hasChanged(MegaChatMessage::CHANGE_TYPE_CONTENT).
     *
     * If the edit is rejected because the original message is too old, this function return NULL.
     *
     * When an already delivered message (MegaChatMessage::STATUS_DELIVERED) is edited, the status
     * of the message will change from STATUS_SENDING directly to STATUS_DELIVERED again, without
     * the transition through STATUS_SERVER_RECEIVED. In other words, the protocol doesn't allow
     * to know when an edit has been delivered to the target user, but only when the edit has been
     * received by the server, so for convenience the status of the original message is kept.
     * if MegaChatApi::isMessageReceptionConfirmationActive returns false, messages may never
     * reach the status delivered, since the target user will not send the required acknowledge to the
     * server upon reception.
     *
     * After this function, MegaChatApi::sendStopTypingNotification has to be called. To notify other clients
     * that it isn't typing
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     * @param longitude from shared geolocation
     * @param latitude from shared geolocation
     * @param img Preview as a byte array encoded in Base64URL. It can be NULL
     * @return ChatMessage that will be sent. The message id is not definitive, but temporal.
     */
    suspend fun editGeolocation(
        chatId: Long,
        msgId: Long,
        longitude: Float,
        latitude: Float,
        img: String,
    ): ChatMessage?

    /**
     * Gets the original path of this node if it has been cached during the upload before attaching the node.
     *
     * @param nodeId The [NodeId] of the node.
     * @return The cached original path, or null if not cached.
     */
    fun getCachedOriginalPathForNode(nodeId: NodeId): String?

    /**
     * Caches the original path of this node once the original file is uploaded, just before attaching it to the chat.
     *
     * @param nodeId The [NodeId] of the node.
     * @param path The original path to be cached.
     */
    fun cacheOriginalPathForNode(nodeId: NodeId, path: String)

    /**
     * Gets the original path of this pending message if it has been cached during the creation of the pending message.
     *
     * @param pendingMessageId The id of the node.
     * @return The cached original path or uri, or null if not cached.
     */
    fun getCachedOriginalPathForPendingMessage(pendingMessageId: Long): String?

    /**
     * Caches the original path of the pending message once the pending message is created, before it is copied to cache folder and/or scaled/compressed.
     *
     * @param pendingMessageId The id of the pending message.
     * @param path The original path or uri to be cached.
     */
    fun cacheOriginalPathForPendingMessage(pendingMessageId: Long, path: String)

    /**
     * Get paged messages
     *
     * @param chatId
     * @return flow of paged messages
     */
    fun getPagedMessages(chatId: Long): PagingSource<Int, TypedMessage>

    /**
     * Deletes all messages in a chat that have a timestamp older than the truncate timestamp
     *
     * @param chatId
     * @param truncateTimestamp
     */
    suspend fun truncateMessages(chatId: Long, truncateTimestamp: Long)

    /**
     * Delete all pending messages in a chat.
     *
     * @param chatId
     */
    suspend fun clearChatPendingMessages(chatId: Long)

    /**
     * Remove sent message
     *
     * @param message
     */
    suspend fun removeSentMessage(message: UserMessage)

    /**
     * Update does not exists in message.
     *
     * @param chatId Chat id.
     * @param msgId Message id.
     */
    suspend fun updateDoesNotExistInMessage(chatId: Long, msgId: Long)

    /**
     * Get exists in message
     *
     * @param chatId Chat id.
     * @param msgId Message id.
     * @return Whether the content in message exists.
     */
    suspend fun getExistsInMessage(chatId: Long, msgId: Long): Boolean

    /**
     * Clear all data from chat database
     */
    suspend fun clearAllData()

    fun updatePendingMessagesCompressionProgress(
        progress: Progress,
        pendingMessages: List<PendingMessage>,
    )

    fun monitorPendingMessagesCompressionProgress(): Flow<Map<Long, Progress>>

    fun clearPendingMessagesCompressionProgress()

}