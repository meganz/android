package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.ConnectionState
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Chat repository
 */
interface ChatRepository {
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
     * Leave chat
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest]
     */
    suspend fun leaveChat(
        chatId: Long,
    ): ChatRequest

    /**
     * Get chat files folder id if it exists
     */
    suspend fun getChatFilesFolderId(): NodeId?

    /**
     * Get meeting chat rooms
     *
     * @return  List of [ChatRoom]
     */
    suspend fun getMeetingChatRooms(): List<CombinedChatRoom>?

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
}
