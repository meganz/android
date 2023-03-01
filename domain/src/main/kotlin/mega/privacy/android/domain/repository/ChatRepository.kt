package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.meeting.ResultOccurrenceUpdate
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
     * Get a scheduled meeting given a chatId and a scheduled meeting id
     *
     * @param chatId  MegaChatHandle that identifies a chat room
     * @param scheduledMeetingId MegaChatHandle that identifies a scheduled meeting
     * @return The scheduled meeting.
     */
    suspend fun getScheduledMeeting(chatId: Long, scheduledMeetingId: Long): ChatScheduledMeeting?

    /**
     * Get a list of all scheduled meeting for a chatroom
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @return List of scheduled meeting.
     */
    suspend fun getScheduledMeetingsByChat(chatId: Long): List<ChatScheduledMeeting>?

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
     * Get all scheduled meetings
     *
     * @return List of scheduled meetings
     */
    suspend fun getAllScheduledMeetings(): List<ChatScheduledMeeting>?

    /**
     * Get a list of all scheduled meeting occurrences for a chatroom
     *
     * @param chatId  MegaChatHandle that identifies a chat room
     * @param count   Number of occurrences to retrieve
     * @return The list of scheduled meetings occurrences.
     */
    suspend fun fetchScheduledMeetingOccurrencesByChat(
        chatId: Long,
        count: Int = 20,
    ): List<ChatScheduledMeetingOccurr>

    /**
     * Get a list of all scheduled meeting occurrences for a chatroom
     *
     * @param chatId    MegaChatHandle that identifies a chat room
     * @param since     Timestamp from which API will generate more occurrences
     * @return The list of scheduled meetings occurrences.
     */
    suspend fun fetchScheduledMeetingOccurrencesByChat(
        chatId: Long,
        since: Long,
    ): List<ChatScheduledMeetingOccurr>

    /**
     * Get next available scheduled meeting occurrence given the current time
     *
     * @param chatId    MegaChatHandle that identifies a chat room
     * @return          ChatScheduledMeetingOccurr
     */
    suspend fun getNextScheduledMeetingOccurrence(chatId: Long): ChatScheduledMeetingOccurr?

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
    suspend fun monitorChatRoomUpdates(chatId: Long): Flow<ChatRoom>

    /**
     * Monitor updates on scheduled meetings
     *
     * @return          A flow of [ChatScheduledMeeting]
     */
    suspend fun monitorScheduledMeetingUpdates(): Flow<ChatScheduledMeeting>

    /**
     * Monitor updates on scheduled meeting occurrences
     *
     * @return          A flow of ResultOccurrenceUpdate
     */
    suspend fun monitorScheduledMeetingOccurrencesUpdates(): Flow<ResultOccurrenceUpdate>

    /**
     * Monitor updates on chat list item.
     *
     * @return A flow of [ChatListItem].
     */
    suspend fun monitorChatListItemUpdates(): Flow<ChatListItem>

    /**
     * Monitor chat call updates
     *
     * @return A flow of [ChatCall]
     */
    suspend fun monitorChatCallUpdates(): Flow<ChatCall>

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
     * Monitor muted chats
     *
     * @return  A flow of Booleans indicating some changes has been made
     */
    fun monitorMutedChats(): Flow<Boolean>

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
}
