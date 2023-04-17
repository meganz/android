package mega.privacy.android.data.gateway.api

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.ChatCallUpdate
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.ScheduledMeetingUpdate
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledRules
import nz.mega.sdk.MegaHandleList

/**
 * Mega chat api gateway
 *
 * @constructor Create empty Mega chat api gateway
 */
interface MegaChatApiGateway {

    /**
     * Init state
     */
    val initState: Int

    /**
     * Initializes API.
     *
     * @param session   Account session.
     * @return Init state.
     */
    fun init(session: String?): Int

    /**
     * Logouts API.
     */
    fun logout(listener: MegaChatRequestListenerInterface?)

    /**
     * Set logger
     *
     * @param logger
     */
    fun setLogger(logger: MegaChatLoggerInterface)

    /**
     * Set log level
     *
     * @param logLevel
     */
    fun setLogLevel(logLevel: Int)

    /**
     * Add chat request listener
     *
     * @param listener
     */
    fun addChatRequestListener(listener: MegaChatRequestListenerInterface)

    /**
     * Remove chat request listener
     *
     * @param listener
     */
    fun removeChatRequestListener(listener: MegaChatRequestListenerInterface)

    /**
     * Notifies a push has been received.
     *
     * @param beep      True if should beep, false otherwise.
     * @param listener  Listener.
     */
    fun pushReceived(beep: Boolean, listener: MegaChatRequestListenerInterface?)

    /**
     * Refreshes DNS servers and retries pending connections.
     *
     * @param disconnect True if should disconnect, false otherwise.
     * @param listener Listener
     */
    fun retryPendingConnections(disconnect: Boolean, listener: MegaChatRequestListenerInterface?)

    /**
     * Chat updates.
     */
    val chatUpdates: Flow<ChatUpdate>

    /**
     * Chat call updates
     */
    val chatCallUpdates: Flow<ChatCallUpdate>

    /**
     * Request the number of minutes since the user was seen as green by last time.
     *
     * @param userHandle User handle from who the last green has been requested.
     */
    suspend fun requestLastGreen(userHandle: Long)

    /**
     * Creates a chat for one or more participants, allowing you to specify their
     * permissions and if the chat should be a group chat or not.
     *
     * @param isGroup  True if is should create a group chat, false otherwise.
     * @param peers    [MegaChatPeerList] including contacts and their privilege level.
     * @param listener Listener.
     */
    fun createChat(
        isGroup: Boolean,
        peers: MegaChatPeerList,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     *  Leave a chat room
     *
     * @param chatId    Chat id
     * @param listener  Listener
     */
    fun leaveChat(
        chatId: Long,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Set open invite setting for a chat room, allowing participants to add more participants to the chat.
     *
     * @param chatId  The chat id.
     * @param enabled  True if allow add participants, false otherwise.
     * @param listener Listener.
     */
    fun setOpenInvite(
        chatId: Long,
        enabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Get meeting chat rooms
     *
     * @return  The list of chat rooms
     */
    fun getMeetingChatRooms(): List<MegaChatRoom>?

    /**
     * Gets a 1to1 chat conversation if exists.
     *
     * @param userHandle The user handle.
     * @return The chat conversation.
     */
    fun getChatRoomByUser(userHandle: Long): MegaChatRoom?

    /**
     * Request user attributes
     *
     * This function is useful to get the email address, first name, last name and full name
     * from chat link participants that they are not loaded
     *
     * After request is finished, you can call to MegaChatApi::getUserFirstnameFromCache,
     * MegaChatApi::getUserLastnameFromCache, MegaChatApi::getUserFullnameFromCache,
     * MegaChatApi::getUserEmailFromCache (email will not available in anonymous mode)
     *
     * @param chatId Handle of the chat whose member attributes requested
     * @param userList List of user whose attributes has been requested
     * @param listener MegaChatRequestListener to track this request
     */
    fun loadUserAttributes(
        chatId: Long,
        userList: MegaHandleList,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Get user email from cache
     *
     * @param   userHandle User handle.
     * @return  User email or null if it has not been cached yet.
     */
    fun getUserEmailFromCache(userHandle: Long): String?

    /**
     * Get the alias given to the user from cache.
     *
     * @param   userHandle User handle.
     * @return  User alias or null if it has not been cached yet.
     */
    fun getUserAliasFromCache(userHandle: Long): String?

    /**
     * Get user first name from cache.
     *
     * @param   userHandle User handle.
     * @return  User firstname or null if it has not been cached yet.
     */
    fun getUserFirstnameFromCache(userHandle: Long): String?

    /**
     * Get user last name from cache.
     *
     * @param   userHandle User handle.
     * @return  User lastname or null if it has not been cached yet.
     */
    fun getUserLastnameFromCache(userHandle: Long): String?

    /**
     *  Get user full name from cache.
     *
     * @param   userHandle User handle.
     * @return  User full name or null if it has not been cached yet.
     */
    fun getUserFullNameFromCache(userHandle: Long): String?

    /**
     * Gets the online status of a user.
     *
     * @param userHandle Handle of user whose status is requested.
     * @return Online status of the user.
     */
    fun getUserOnlineStatus(userHandle: Long): Int

    /**
     * Gets chat Room updates.
     */
    fun getChatRoomUpdates(chatId: Long): Flow<ChatRoomUpdate>

    /**
     * Gets a chat conversation if exists.
     *
     * @param chatId The chat id.
     * @return The chat conversation.
     */
    fun getChatRoom(chatId: Long): MegaChatRoom?

    /**
     * Get chat list item.
     *
     * @param chatId    Chat Id
     * @return          Chat list item
     */
    fun getChatListItem(chatId: Long): MegaChatListItem?

    /**
     * Gets the MegaChatCall
     *
     * @param chatId The chat id.
     * @return the appropriate call.
     */
    fun getChatCall(chatId: Long): MegaChatCall?

    /**
     * Start new call.
     *
     * @param chatId  The chat id.
     * @param enabledVideo True for audio-video call, false for audio call.
     * @param enabledAudio True for starting a call with audio (mute disabled).
     * @param listener Listener.
     */
    fun startChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Start scheduled meeting.
     *
     * @param chatId        The chat id.
     * @param schedId       The scheduled meeting id.
     * @param enabledVideo  True for audio-video call, false for audio call.
     * @param enabledAudio  True for starting a call with audio (mute disabled).
     * @param listener      Listener.
     */
    fun startChatCallNoRinging(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Answer call.
     *
     * @param chatId  The chat id.
     * @param enabledVideo True for audio-video call, false for audio call.
     * @param enabledAudio True for answering a call with audio (mute disabled).
     * @param listener Listener.
     */
    fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Hang call.
     *
     * @param callId  The call id.
     * @param listener Listener.
     */
    fun hangChatCall(
        callId: Long,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Select the video device to be used in calls.
     *
     * @param device Identifier of device to be selected.
     * @param listener Listener.
     */
    fun setChatVideoInDevice(
        device: String,
        listener: MegaChatRequestListenerInterface?,
    )

    /**
     * Scheduled meetings updates.
     */
    val scheduledMeetingUpdates: Flow<ScheduledMeetingUpdate>

    /**
     * Get all scheduled meetings
     *
     * @return  List of scheduled meetings
     */
    fun getAllScheduledMeetings(): List<MegaChatScheduledMeeting>?

    /**
     * Get a scheduled meeting given a chatId and a scheduled meeting id
     *
     * @param chatId  MegaChatHandle that identifies a chat room
     * @param schedId MegaChatHandle that identifies a scheduled meeting
     * @return The scheduled meeting.
     */
    fun getScheduledMeeting(chatId: Long, schedId: Long): MegaChatScheduledMeeting?

    /**
     * Get a list of all scheduled meeting for a chatroom
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @return List of scheduled meetings.
     */
    fun getScheduledMeetingsByChat(chatId: Long): List<MegaChatScheduledMeeting>?

    /**
     * Get a list of all scheduled meeting occurrences for a chatroom
     *
     * @param chatId  MegaChatHandle that identifies a chat room
     * @param since     Timestamp from which API will generate more occurrences
     * @param listener MegaChatRequestListener to track this request
     * @return The list of scheduled meetings occurrences.
     */
    fun fetchScheduledMeetingOccurrencesByChat(
        chatId: Long,
        since: Long,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Invite contact to a chat
     *
     * @param chatId        Chat id.
     * @param userHandle    User handle.
     * @param listener      Listener.
     */
    fun inviteToChat(chatId: Long, userHandle: Long, listener: MegaChatRequestListenerInterface?)

    /**
     * Get basic information about a public chat.
     *
     * @param link      Public chat link.
     * @param listener  Listener.
     */
    fun checkChatLink(link: String, listener: MegaChatRequestListenerInterface?)

    /**
     * Set the chat mode to private
     * @param chatId    Chat id.
     * @param listener  Listener.
     */
    fun setPublicChatToPrivate(
        chatId: Long,
        listener: MegaChatRequestListenerInterface?,
    )

    /**
     * Query chat link
     *
     * @param chatId        Chat id.
     * @param listener      Listener.
     */
    fun queryChatLink(chatId: Long, listener: MegaChatRequestListenerInterface?)

    /**
     * Remove chat link
     *
     * @param chatId        Chat id.
     * @param listener      Listener.
     */
    fun removeChatLink(chatId: Long, listener: MegaChatRequestListenerInterface?)

    /**
     * Create chat link
     *
     * @param chatId        Chat id.
     * @param listener      Listener.
     */
    fun createChatLink(chatId: Long, listener: MegaChatRequestListenerInterface?)

    /**
     * Get my user handle
     *
     * @return My user handle
     */
    fun getMyUserHandle(): Long

    /**
     * Get my full name
     *
     * @return My full name
     */
    fun getMyFullname(): String?

    /**
     * Get my email
     *
     * @return My full email
     */
    fun getMyEmail(): String

    /**
     * Get Chat Invalid Handle
     */
    fun getChatInvalidHandle(): Long

    /**
     * Get my chat status
     */
    fun getOnlineStatus(): Int

    /**
     *  Remove participant from chat
     *
     * @param chatId    Chat id
     * @param handle    User handle
     * @param listener  Listener
     */
    fun removeFromChat(
        chatId: Long,
        handle: Long,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Update participant permissions
     * @param chatId        Chat id.
     * @param handle        User handle.
     * @param privilege     User privilege.
     * @param listener      Listener.
     */
    fun updateChatPermissions(
        chatId: Long,
        handle: Long,
        privilege: Int,
        listener: MegaChatRequestListenerInterface?,
    )

    /**
     * Gets the MegaChatMessage specified from the chat room.
     *
     * This function allows to retrieve only those messages that are been loaded, received and/or
     * sent (confirmed and not yet confirmed). For any other message, this function
     * will return NULL.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId    MegaChatHandle that identifies the chat room
     * @param messageId MegaChatHandle that identifies the message
     * @return The MegaChatMessage object, or NULL if not found.
     */
    fun getMessage(chatId: Long, messageId: Long): MegaChatMessage?

    /**
     * Gets the MegaChatMessage specified from the chat room stored in node history
     *
     * This function allows to retrieve only those messages that are in the node history
     *
     * You take the ownership of the returned value.
     *
     * @param chatId    MegaChatHandle that identifies the chat room
     * @param messageId MegaChatHandle that identifies the message
     * @return The MegaChatMessage object, or NULL if not found.
     */
    fun getMessageFromNodeHistory(chatId: Long, messageId: Long): MegaChatMessage?

    /**
     * Removes chat request listener.
     */
    fun removeRequestListener(listener: MegaChatRequestListenerInterface)

    /**
     * Returns whether the autoaway mechanism is active.
     *
     * This function may return false even when the Presence settings
     * establish that autoaway option is active. It happens when the persist
     * option is enabled and when the status is offline or away.
     *
     * @return  True if the app should call [signalPresenceActivity], false otherwise
     */
    fun isSignalActivityRequired(): Boolean

    /**
     * Signal there is some user activity
     *
     * When the presence configuration is set to autoaway (and persist is false), this
     * function should be called regularly to not turn into away status automatically.
     *
     * A good approach is to call this function with every mouse move or keypress on desktop
     * platforms; or at any finger tap or gesture and any keypress on mobile platforms.
     *
     * Failing to call this function, you risk a user going "Away" while typing a lengthy message,
     * which would be awkward.
     *
     * The associated request type with this request is [TYPE_SIGNAL_ACTIVITY].
     *
     * @param listener  [MegaChatRequestListenerInterface] to track this request
     */
    fun signalPresenceActivity(listener: MegaChatRequestListenerInterface)

    /**
     * Allows to un/archive chats
     *
     * This is a per-chat and per-user option, and it's intended to be used when the user does
     * not care anymore about an specific chatroom. Archived chatrooms should be displayed in a
     * different section or alike, so it can be clearly identified as archived.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_ARCHIVE_CHATROOM
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getFlag - Returns if chat is to be archived or unarchived
     *
     * On the onRequestFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ENOENT - If the chatroom doesn't exists.
     * - MegaChatError::ERROR_ACCESS - If caller is not operator.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param archive True to set the chat as archived, false to unarchive it.
     * @param listener [MegaChatRequestListenerInterface] to track this request
     */
    fun archiveChat(chatId: Long, archive: Boolean, listener: MegaChatRequestListenerInterface)

    /**
     * Refresh URLs and establish fresh connections
     *
     * The associated request type with this request is MegaChatRequest::TYPE_RETRY_PENDING_CONNECTIONS
     *
     * A disconnect will be forced automatically, followed by a reconnection to the fresh URLs
     * retrieved from API. This parameter is useful when the URL for the API is changed
     * via MegaApi::changeApiUrl.
     */
    suspend fun refreshUrl()

    /**
     * Get a list with the ids of chat-rooms where there are active calls
     *
     * The list of ids can be retrieved for calls in one specific state by setting
     * the parameter callState. If state is -1, it returns all calls regardless their state.
     *
     * You take the ownership of the returned value.
     *
     * @param state of calls that you want receive, -1 to consider all states
     * @return A list of handles with the ids of chat-rooms where there are active calls
     */
    fun getChatCalls(state: Int): MegaHandleList?

    /**
     * Creates a chatroom and a scheduled meeting for that chatroom
     *
     * @param isMeeting True to create a meeting room, otherwise false
     * @param publicChat True to create a public chat, otherwise false
     * @param title Null-terminated character string with the scheduled meeting title. Maximum allowed length is MegaChatScheduledMeeting::MAX_TITLE_LENGTH characters
     * @param speakRequest True to set that during calls non moderator users, must request permission to speak
     * @param waitingRoom True to set that during calls, non moderator members will be placed into a waiting room.
     * A moderator user must grant each user access to the call.
     * @param openInvite to set that users with MegaChatRoom::PRIV_STANDARD privilege, can invite other users into the chat
     * @param timezone Timezone where we want to schedule the meeting
     * @param startDate start date time of the meeting with the format (unix timestamp UTC)
     * @param endDate end date time of the meeting with the format (unix timestamp UTC)
     * @param description Null-terminated character string with the scheduled meeting description. Maximum allowed length is MegaChatScheduledMeeting::MAX_DESC_LENGTH characters
     * Note that description is a mandatory field, so in case you want to set an empty description, please provide an empty string with Null-terminated character at the end
     * @param flags Scheduled meeting flags to establish scheduled meetings flags like avoid email sending (Check MegaChatScheduledFlags class)
     * @param rules Repetition rules for creating a recurrent meeting (Check MegaChatScheduledRules class)
     * @param attributes - not supported yet
     * @param listener MegaChatRequestListener to track this request
     */
    fun createChatroomAndSchedMeeting(
        peerList: MegaChatPeerList,
        isMeeting: Boolean,
        publicChat: Boolean,
        title: String,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
        timezone: String,
        startDate: Long,
        endDate: Long,
        description: String,
        flags: MegaChatScheduledFlags?,
        rules: MegaChatScheduledRules?,
        attributes: String,
        listener: MegaChatRequestListenerInterface,
    )
}
