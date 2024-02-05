package mega.privacy.android.domain.repository.chat

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
}