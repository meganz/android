package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.NotificationBehaviour
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.ConnectionState
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Chat repository
 */
interface ChatRepository {
    /**
     * Get chat init state
     * @return Init state as [ChatInitState]
     */
    suspend fun getChatInitState(): ChatInitState

    /**
     * Init chat API as anonymous user
     * @return Init state as [ChatInitState]
     */
    suspend fun initAnonymousChat(): ChatInitState

    /**
     * Notify chat logout
     *
     * @return a flow that emits true whenever chat api is successfully logged out
     */
    fun notifyChatLogout(): Flow<Boolean>

    /**
     * Gets chat room if it exists
     *
     * @param chatId      Chat Id
     * @return [ChatRoom] containing the updated data.
     */
    suspend fun getChatRoom(chatId: Long): ChatRoom?

    /**
     * Gets chat room if it exists
     *
     * @param userHandle      User handle
     * @return [ChatRoom] containing the updated data.
     */
    suspend fun getChatRoomByUser(userHandle: Long): ChatRoom?

    /**
     * Get chat list item if it exsits
     *
     * @param chatId    Chat Id
     * @return          [ChatListItem]
     */
    suspend fun getChatListItem(chatId: Long): ChatListItem?

    /**
     * Get all chat list items
     *
     * @return  Chat List items
     */
    suspend fun getAllChatListItems(): List<ChatListItem>

    /**
     * Update open invite setting.
     *
     * @param chatId   The Chat id.
     * @return True if non-hosts are allowed to add participants, false otherwise.
     */
    suspend fun setOpenInvite(chatId: Long): Boolean

    /**
     * Update open invite setting.
     *
     * @param chatId   The Chat id.
     * @param isOpenInvite True if non-hosts are allowed to add participants, false otherwise.
     */
    suspend fun setOpenInvite(chatId: Long, isOpenInvite: Boolean): ChatRequest

    /**
     * Update waiting room setting.
     *
     * @param chatId    The Chat id.
     * @param enabled   True, should be enabled. False, should be disabled.
     * @return True if non-hosts are allowed to add participants, false otherwise.
     */
    suspend fun setWaitingRoom(chatId: Long, enabled: Boolean): ChatRequest

    /**
     * Leave chat
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest]
     */
    suspend fun leaveChat(
        chatId: Long,
    ): ChatRequest

    /**
     * Update chat title.
     *
     * @param chatId    The Chat id.
     * @param title     Title.
     */
    suspend fun setChatTitle(chatId: Long, title: String): ChatRequest

    /**
     * Get chat files folder id if it exists
     */
    suspend fun getChatFilesFolderId(): NodeId?

    /**
     * Get all chat rooms
     *
     * @return  List of [CombinedChatRoom]
     */
    suspend fun getAllChatRooms(): List<CombinedChatRoom>

    /**
     * Get meeting chat rooms
     *
     * @return  List of [CombinedChatRoom]
     */
    suspend fun getMeetingChatRooms(): List<CombinedChatRoom>

    /**
     * Get non meeting chat rooms
     *
     * @return  List of [CombinedChatRoom]
     */
    suspend fun getNonMeetingChatRooms(): List<CombinedChatRoom>

    /**
     * Get Archived chat rooms
     *
     * @return  List of [CombinedChatRoom]
     */
    suspend fun getArchivedChatRooms(): List<CombinedChatRoom>

    /**
     * Gets combined chat room if it exists
     *
     * @param chatId      Chat Id
     * @return [CombinedChatRoom] containing the updated data.
     */
    suspend fun getCombinedChatRoom(chatId: Long): CombinedChatRoom?

    /**
     * Invite contacts to chat.
     *
     * @param chatId            The Chat id.
     * @param contactsData      List of contacts to add
     */
    suspend fun inviteToChat(chatId: Long, contactsData: List<String>)

    /**
     * Invite participant to chat.
     *
     * @param chatId            The Chat id.
     * @param , handle          User handle.
     */
    suspend fun inviteParticipantToChat(chatId: Long, handle: Long): ChatRequest

    /**
     * Set public chat to private.
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest].
     */
    suspend fun setPublicChatToPrivate(
        chatId: Long,
    ): ChatRequest

    /**
     * Create chat link.
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest]
     */
    suspend fun createChatLink(chatId: Long): ChatRequest

    /**
     * Remove chat link.
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest]
     */
    suspend fun removeChatLink(chatId: Long): ChatRequest

    /**
     * Obtain basic information abouts a public chat.
     *
     * @param link  Public chat link.
     * @return      [ChatRequest].
     */
    suspend fun checkChatLink(
        link: String,
    ): ChatRequest

    /**
     * Query chat link.
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest]
     */
    suspend fun queryChatLink(chatId: Long): ChatRequest

    /**
     * Update chat permissions
     *
     * @param chatId        The chat id.
     * @param handle        User handle.
     * @param permission    User privilege.
     * @return              The Chat Request.
     */
    suspend fun updateChatPermissions(
        chatId: Long,
        handle: Long,
        permission: ChatRoomPermission,
    ): ChatRequest

    /**
     * Remove participant from chat
     *
     * @param chatId    The Chat id.
     * @param handle    User handle
     * @return          [ChatRequest]
     */
    suspend fun removeFromChat(
        chatId: Long,
        handle: Long,
    ): ChatRequest

    /**
     * Invite contact
     *
     * @param email    User email
     * @return
     */
    suspend fun inviteContact(
        email: String,
    ): InviteContactRequest

    /**
     * Monitor updates on chat room item update
     *
     * @param chatId    Chat Id.
     * @return          A flow of [ChatRoom]
     */
    fun monitorChatRoomUpdates(chatId: Long): Flow<ChatRoom>

    /**
     * Load messages from a chat room
     *
     * @param chatId    Chat ID
     * @param count     The number of requested messages to load (Range 1 - 256)
     *
     * @return The source of the messages that is going to be fetched. The possible values are:
     *   - ChatHistoryLoadStatus::ERROR: history has to be fetched from server, but we are not logged in yet
     *   - ChatHistoryLoadStatus::NONE: there's no more history available (not even in the server)
     *   - ChatHistoryLoadStatus::LOCAL: messages will be fetched locally (RAM or DB)
     *   - ChatHistoryLoadStatus::REMOTE: messages will be requested to the server. Expect some delay
     *
     * The value ChatHistoryLoadStatus::REMOTE can be used to show a progress bar accordingly when network operation occurs.
     */
    suspend fun loadMessages(chatId: Long, count: Int): ChatHistoryLoadStatus

    /**
     * Monitor message load on a chat room
     *
     * @param chatId    Chat ID.
     * @return          A flow of [ChatMessage]
     */
    fun monitorOnMessageLoaded(chatId: Long): Flow<ChatMessage?>

    /**
     * Monitor updates on chat list item.
     *
     * @return A flow of [ChatListItem].
     */
    fun monitorChatListItemUpdates(): Flow<ChatListItem>

    /**
     * Returns whether notifications about a chat have to be generated.
     *
     * @param chatId    Chat id
     * @return          True if notifications has to be created, false otherwise.
     */
    suspend fun isChatNotifiable(chatId: Long): Boolean

    /**
     * Check if chat last message geolocation
     *
     * @param chatId    Chat id
     * @return          True if last message is geolocation meta type or false otherwise
     */
    suspend fun isChatLastMessageGeolocation(chatId: Long): Boolean

    /**
     * Get my email updated
     */
    fun monitorMyEmail(): Flow<String?>

    /**
     * Get my name updated
     */
    fun monitorMyName(): Flow<String?>

    /**
     * Reset chat settings if not initialized yet.
     */
    suspend fun resetChatSettings()

    /**
     * Signal chat presence activity
     */
    suspend fun signalPresenceActivity()

    /**
     * Clear chat history
     *
     * @param chatId    Chat id
     */
    suspend fun clearChatHistory(chatId: Long)

    /**
     * Archive chat
     *
     * @param chatId    Chat id
     * @param archive   True to archive, false to unarchive
     */
    suspend fun archiveChat(chatId: Long, archive: Boolean)

    /**
     * Get Peer Handle
     *
     * @param chatId id of the chat
     * @param peerNo required peer number
     * @return peer handle for the selected peer [Long]
     */
    suspend fun getPeerHandle(chatId: Long, peerNo: Long): Long?

    /**
     * Creates a chat if not existing
     *
     * @param isGroup     True if is should create a group chat, false otherwise.
     * @param userHandles List of contact handles.
     * @return The chat conversation handle.
     */
    suspend fun createChat(isGroup: Boolean, userHandles: List<Long>): Long

    /**
     * Returns the current state of the connection
     *
     * @return [ConnectionState]
     */
    suspend fun getConnectionState(): ConnectionState

    /**
     * Returns the current state of the connection to chatId
     *
     * @param chatId
     * @return [ChatConnectionStatus]
     */
    suspend fun getChatConnectionState(chatId: Long): ChatConnectionStatus

    /**
     * Monitor chat archived.
     *
     * @return Flow [String]
     */
    fun monitorChatArchived(): Flow<String>

    /**
     * Broadcast chat archived.
     *
     * @param chatTitle [String]
     */
    suspend fun broadcastChatArchived(chatTitle: String)

    /**
     * Gets the number of unread chats for the logged in user.
     *
     * @return Number of unread chats.
     */
    suspend fun getNumUnreadChats(): Int

    /**
     * Monitor if successfully joined to a chat.
     *
     * @return Flow [Boolean]
     */
    fun monitorJoinedSuccessfully(): Flow<Boolean>

    /**
     * Broadcast if successfully joined to a chat.
     */
    suspend fun broadcastJoinedSuccessfully()

    /**
     * Monitor if should leave a chat.
     *
     * @return Flow [Long] ID of the chat to leave.
     */
    fun monitorLeaveChat(): Flow<Long>

    /**
     * Broadcast that should leave a chat.
     *
     * @param chatId [Long] ID of the chat to leave.
     */
    suspend fun broadcastLeaveChat(chatId: Long)

    /**
     * Returns the [ChatMessage] specified from the chat room.
     *
     * This function allows to retrieve only those messages that are been loaded, received and/or
     * sent (confirmed and not yet confirmed). For any other message, this function
     * will return NULL.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     * @return The [ChatMessage] object, or NULL if not found.
     */
    suspend fun getMessage(chatId: Long, msgId: Long): ChatMessage?

    /**
     * Returns the [ChatMessage] specified from the chat room stored in node history
     *
     * This function allows to retrieve only those messages that are in the node history
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     * @return The [ChatMessage] object, or NULL if not found.
     */
    suspend fun getMessageFromNodeHistory(chatId: Long, msgId: Long): ChatMessage?

    /**
     * Gets chat message notification behaviour.
     *
     * @param beep Push notification flag indicating if the notification should beep or not.
     * @param defaultSound Default device sound.
     * @return [NotificationBehaviour]
     */
    suspend fun getChatMessageNotificationBehaviour(
        beep: Boolean,
        defaultSound: String?,
    ): NotificationBehaviour

    /**
     * Gets pending messages.
     *
     * @param chatId Chat identifier from which the messages has to be get.
     * @return A list of [PendingMessage].
     */
    suspend fun getPendingMessages(chatId: Long): List<PendingMessage>

    /**
     * Updates a pending message.
     *
     * @param idMessage   Identifier of the pending message.
     * @param transferTag Identifier of the transfer.
     * @param nodeHandle  Handle of the node already uploaded.
     * @param state       State of the pending message.
     */
    suspend fun updatePendingMessage(
        idMessage: Long,
        transferTag: Int,
        nodeHandle: String?,
        state: Int,
    )
}
