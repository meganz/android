package mega.privacy.android.data.gateway.api

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.ScheduledMeetingUpdate
import mega.privacy.android.data.model.meeting.ChatCallUpdate
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
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
import nz.mega.sdk.MegaChatVideoListenerInterface
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
     * Initializes API as anonymous user
     * @return Init state.
     */
    fun initAnonymous(): Int

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
     * @param chatId    Chat identifier.
     * @param listener  Listener.
     */
    fun pushReceived(beep: Boolean, chatId: Long, listener: MegaChatRequestListenerInterface?)

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
     * Creates a group chat for one or more participants, allowing you to specify their
     * permissions and creation chat options
     *
     * @param title Null-terminated character string with the chat title. If the title
     * is longer than 30 characters, it will be truncated to that maximum length.
     * @param peers MegaChatPeerList including other users and their privilege level
     * @param speakRequest True to set that during calls non moderator users, must request permission to speak
     * @param waitingRoom True to set that during calls, non moderator members will be placed into a waiting room.
     * A moderator user must grant each user access to the call.
     * @param openInvite to set that users with MegaChatRoom::PRIV_STANDARD privilege, can invite other users into the chat
     * @param listener MegaChatRequestListener to track this request
     */
    fun createGroupChat(
        peers: MegaChatPeerList,
        title: String?,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Creates an public chatroom for multiple participants (groupchat) allowing
     * you to specify creation chat options
     *
     * @param title Null-terminated character string with the chat title. If the title
     * is longer than 30 characters, it will be truncated to that maximum length.
     * @param peers MegaChatPeerList including other users and their privilege level
     * @param speakRequest True to set that during calls non moderator users, must request permission to speak
     * @param waitingRoom True to set that during calls, non moderator members will be placed into a waiting room.
     * A moderator user must grant each user access to the call.
     * @param openInvite to set that users with MegaChatRoom::PRIV_STANDARD privilege, can invite other users into the chat
     * @param listener MegaChatRequestListener to track this request
     */
    fun createPublicChat(
        peers: MegaChatPeerList,
        title: String?,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
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
     * @param chatId    The chat id.
     * @param title     Chat title.
     * @param listener Listener.
     */
    fun setChatTitle(
        chatId: Long,
        title: String,
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
     * Set waiting room setting for a chat room
     *
     * @param chatId  The chat id.
     * @param enabled  True if allow add participants, false otherwise.
     * @param listener Listener.
     */
    fun setWaitingRoom(
        chatId: Long,
        enabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Get all chat rooms
     *
     * @return  The list of chat rooms
     */
    fun getChatRooms(): List<MegaChatRoom>

    /**
     * Get meeting chat rooms
     *
     * @return  The list of chat rooms
     */
    fun getMeetingChatRooms(): List<MegaChatRoom>?

    /**
     * Get group chat rooms
     *
     * @return  The list of chat rooms
     */
    fun getGroupChatRooms(): List<MegaChatRoom>?

    /**
     * Get individual chat rooms
     *
     * @return  The list of chat rooms
     */
    fun getIndividualChatRooms(): List<MegaChatRoom>?

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
     * Opens a chat room and starts getting updates.
     */
    fun openChatRoom(chatId: Long): Flow<ChatRoomUpdate>

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
     * Get chat list items
     *
     * @param mask      Values to apply in the filter
     * @param filter    Filters to apply to the list of chats
     * @return          Chat list items
     */
    fun getChatListItems(mask: Int, filter: Int): List<MegaChatListItem>?

    /**
     * Gets the MegaChatCall
     *
     * @param chatId The chat id.
     * @return the appropriate call.
     */
    fun getChatCall(chatId: Long): MegaChatCall?

    /**
     * Get the MegaChatCall that has a specific id
     *
     * @param callId    Call Id
     * @return          Chat call
     */
    fun getChatCallByCallId(callId: Long): MegaChatCall?

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
     * Get a list with the ids of active calls
     *
     * You take the ownership of the returned value.
     *
     * @return A list of ids of active calls
     */
    fun getChatCallIds(): MegaHandleList?

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
     * Start meeting with waiting room enabled.
     *
     * @param chatId        The chat id.
     * @param schedIdWr     The scheduled meeting id.
     * @param enabledVideo  True for audio-video call, false for audio call.
     * @param enabledAudio  True for starting a call with audio (mute disabled).
     * @param listener      Listener.
     */
    fun startMeetingInWaitingRoomChat(
        chatId: Long,
        schedIdWr: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Ring a user in chatroom with an ongoing call that they didn't pick up
     *
     * When a call is started and one user doesn't pick it up, ringing stops for that user/participant after a given time.
     * This function can be used to force another ringing event at said user/participant.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_RING_INDIVIDUAL_IN_CALL
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle() - Returns the user's id to ring again
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     *
     * The request will fail with MegaChatError::ERROR_ARGS
     * - if chat id provided as param is invalid
     * - if user id to ring again provided as param is invalid
     *
     * The request will fail with MegaChatError::ERROR_NOENT
     * - if the chatroom doesn't exists.
     * - if an ongoing call cannot be found for the chat id provided as a param
     *
     * To receive call notifications, the app needs to register MegaChatCallListener.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param userId MegaChatHandle that identifies the user to ring again
     * @param ringTimeout timeout in seconds (greater than 0) for the call to stop ringing
     * @param listener MegaChatRequestListener to track this request
     */
    fun ringIndividualInACall(
        chatId: Long,
        userId: Long,
        ringTimeout: Int,
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
     * Hold chat call
     *
     * @param chatId        Chat Id
     * @param setOnHold     Flag to set call on hold
     * @param listener      Listener
     */
    fun holdChatCall(
        chatId: Long,
        setOnHold: Boolean,
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
     * Allows any user to preview a public chat without being a participant
     *
     * This function loads the required data to preview a public chat referenced by a
     * chat-link.
     *
     * @param link Null-terminated character string with the public chat link
     * @param listener MegaChatRequestListener to track this request
     */
    fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface?)

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
     * Allow a user to add himself to an existing public chat. To do this the public chat must be in preview mode,
     * the result of a previous call to openChatPreview(), and the public handle contained in the chat-link must be still valid.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_AUTOJOIN_PUBLIC_CHAT
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns invalid handle to identify that is an autojoin
     *
     * On the onRequestFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ARGS  - If the chatroom is not groupal, public or is not in preview mode.
     * - MegaChatError::ERROR_NOENT - If the chat room does not exists, the chatid is not valid or the
     * public handle is not valid.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param listener MegaChatRequestListener to track this request
     */
    fun autojoinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface?)

    /**
     * Allow a user to rejoin to an existing public chat. To do this the public chat
     * must have a valid public handle.
     *
     * This function must be called only after calling:
     * - MegaChatApi::openChatPreview and receive MegaChatError::ERROR_EXIST for a chatroom where
     * your own privilege is MegaChatRoom::PRIV_RM (You are trying to preview a public chat which
     * you were part of, so you have to rejoin it)
     *
     * The associated request type with this request is MegaChatRequest::TYPE_AUTOJOIN_PUBLIC_CHAT
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns the public handle of the chat to identify that
     * is a rejoin
     *
     * On the onRequestFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ARGS - If the chatroom is not groupal, the chatroom is not public
     * or the chatroom is in preview mode.
     * - MegaChatError::ERROR_NOENT - If the chatid is not valid, there isn't any chat with the specified
     * chatid or the chat doesn't have a valid public handle.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param publicHandle MegaChatHandle that corresponds with the public handle of chat room
     * @param listener MegaChatRequestListener to track this request
     */
    fun autorejoinPublicChat(
        chatId: Long,
        publicHandle: Long,
        listener: MegaChatRequestListenerInterface?,
    )

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
     * @brief Returns the email of the logged in user.
     *
     * This function works even in offline mode (MegaChatApi::INIT_OFFLINE_SESSION),
     * since the value is retrieved from cache.
     *
     * You take the ownership of the returned value
     *
     * @return Own user email
     */
    fun getMyEmail(): String?

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
     * Allows a logged in operator/moderator to clear the entire history of a chat
     *
     * The latest message gets overridden with a management message.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_TRUNCATE_HISTORY
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     *
     * On the onTransferFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ACCESS - If the logged in user doesn't have privileges to truncate the chat history
     * - MegaChatError::ERROR_NOENT - If there isn't any chat with the specified chatid.
     * - MegaChatError::ERROR_ARGS - If the chatid or user handle are invalid
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param listener [MegaChatRequestListenerInterface] to track this request
     */
    fun clearChatHistory(chatId: Long, listener: MegaChatRequestListenerInterface)

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
        attributes: String?,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Modify an existing scheduled meeting
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @param schedId MegaChatHandle that identifies the scheduled meeting
     * @param timezone Timezone where we want to schedule the meeting
     * @param startDate start date time of the meeting with the format (unix timestamp UTC)
     * @param endDate end date time of the meeting with the format (unix timestamp UTC)
     * @param title Null-terminated character string with the scheduled meeting title. Maximum allowed length is MegaChatScheduledMeeting::MAX_TITLE_LENGTH characters
     * @param description Null-terminated character string with the scheduled meeting description. Maximum allowed length is MegaChatScheduledMeeting::MAX_DESC_LENGTH characters
     * @param cancelled True if scheduled meeting is going to be cancelled
     * @param flags Scheduled meeting flags to establish scheduled meetings flags like avoid email sending (Check MegaChatScheduledFlags class)
     * @param rules Repetition rules for creating a recurrent meeting (Check MegaChatScheduledRules class)
     * @param updateChatTitle if true chatroom title will be updated along with scheduled meeting title
     */
    fun updateScheduledMeeting(
        chatId: Long,
        schedId: Long,
        timezone: String,
        startDate: Long,
        endDate: Long,
        title: String,
        description: String,
        cancelled: Boolean,
        flags: MegaChatScheduledFlags?,
        rules: MegaChatScheduledRules?,
        updateChatTitle: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Modify an existing scheduled meeting occurrence
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @param schedId MegaChatHandle that identifies the scheduled meeting
     * @param overrides start date time that along with schedId identifies the occurrence with the format (unix timestamp UTC)
     * @param newStartDate new start date time of the occurrence with the format (unix timestamp UTC)
     * @param newEndDate new end date time of the occurrence with the format (unix timestamp UTC)
     * @param cancelled True if scheduled meeting occurrence is going to be cancelled
     */
    fun updateScheduledMeetingOccurrence(
        chatId: Long,
        schedId: Long,
        overrides: Long,
        newStartDate: Long,
        newEndDate: Long,
        cancelled: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Returns the current state of the connection
     *
     * It can be one of the following values:
     *  - MegaChatApi::DISCONNECTED = 0
     *  - MegaChatApi::CONNECTING   = 1
     *  - MegaChatApi::CONNECTED    = 2
     *
     * @return The state of connection
     */
    fun getConnectedState(): Int

    /**
     * Returns the current state of the connection to chatId
     *
     * The possible values are:
     *  - MegaChatApi::CHAT_CONNECTION_OFFLINE      = 0
     *  - MegaChatApi::CHAT_CONNECTION_IN_PROGRESS  = 1
     *  - MegaChatApi::CHAT_CONNECTION_LOGGING      = 2
     *  - MegaChatApi::CHAT_CONNECTION_ONLINE       = 3
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @return The state of connection
     */
    fun getChatConnectionState(chatId: Long): Int

    /**
     * Gets the number of unread chats for the logged in user.
     *
     * @return Number of unread chats.
     */
    suspend fun getNumUnreadChats(): Int

    /**
     * Initiates fetching more history of the specified chatroom.
     *
     * The loaded messages will be notified one by one through the MegaChatRoomListener
     * specified at MegaChatApi::openChatRoom (and through any other listener you may have
     * registered by calling MegaChatApi::addChatRoomListener).
     *
     * The corresponding callback is MegaChatRoomListener::onMessageLoaded.
     *
     * Messages are always loaded and notified in strict order, from newest to oldest.
     *
     * The actual number of messages loaded can be less than "count". One reason is
     * the history being shorter than requested, the other is due to internal protocol
     * messages that are not intended to be displayed to the user. Additionally, if the fetch
     * is local and there's no more history locally available, the number of messages could be
     * lower too (and the next call to MegaChatApi::loadMessages will fetch messages from server).
     *
     * "count" has a maximum value of 256. If user requests more than 256 messages,
     * only 256 messages will returned if exits
     *
     * When there are no more history available from the reported source of messages
     * (local / remote), or when the requested "count" has been already loaded,
     * the callback MegaChatRoomListener::onMessageLoaded will be called with a NULL message.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param count The number of requested messages to load (Range 1 - 256)
     *
     * @return Return the source of the messages that is going to be fetched. The possible values are:
     *   - MegaChatApi::SOURCE_ERROR = -1: history has to be fetched from server, but we are not logged in yet
     *   - MegaChatApi::SOURCE_NONE = 0: there's no more history available (not even in the server)
     *   - MegaChatApi::SOURCE_LOCAL: messages will be fetched locally (RAM or DB)
     *   - MegaChatApi::SOURCE_REMOTE: messages will be requested to the server. Expect some delay
     *
     * The value MegaChatApi::SOURCE_REMOTE can be used to show a progress bar accordingly when network operation occurs.
     */
    suspend fun loadMessages(chatId: Long, count: Int): Int

    /**
     * Set your online status.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_SET_CHAT_STATUS
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaRequest::getNumber - Returns the new status of the user in chat.
     *
     * @param status Online status in the chat.
     *
     * It can be one of the following values:
     * - MegaChatApi::STATUS_OFFLINE = 1
     * The user appears as being offline
     *
     * - MegaChatApi::STATUS_BUSY = 2
     * The user is busy and don't want to be disturbed.
     *
     * - MegaChatApi::STATUS_AWAY = 3
     * The user is away and might not answer.
     *
     * - MegaChatApi::STATUS_ONLINE = 4
     * The user is connected and online.
     *
     * @param listener MegaChatRequestListener to track this request
     */
    fun setOnlineStatus(status: Int, listener: MegaChatRequestListenerInterface)

    /**
     * Register a listener to receive video from local device for an specific chat room
     *
     * You can use MegaChatApi::removeChatLocalVideoListener to stop receiving events.
     *
     * @note if we want to receive video before start a call (openVideoDevice), we have to
     * register a MegaChatVideoListener with chatid = MEGACHAT_INVALID_HANDLE
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param listener MegaChatVideoListener that will receive local video
     */
    fun addChatLocalVideoListener(chatId: Long, listener: MegaChatVideoListenerInterface)

    /**
     * Register a listener to receive video from remote device for an specific chat room and peer
     *
     * You can use MegaChatApi::removeChatRemoteVideoListener to stop receiving events.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param clientId MegaChatHandle that identifies the client
     * @param hiRes boolean that identify if video is high resolution or low resolution
     * @param listener MegaChatVideoListener that will receive remote video
     */
    fun addChatRemoteVideoListener(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MegaChatVideoListenerInterface,
    )

    /**
     * Unregister a MegaChatVideoListener
     *
     * This listener won't receive more events.
     * @note if we want to remove the listener added to receive video frames before start a call
     * we have to use chatid = MEGACHAT_INVALID_HANDLE
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param clientId MegaChatHandle that identifies the client
     * @param hiRes boolean that identify if video is high resolution or low resolution
     * @param listener Object that is unregistered
     */
    fun removeChatVideoListener(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MegaChatVideoListenerInterface?,
    )

    /**
     * Register a listener to receive video from local device for an specific chat room.
     * This listener will be deregistered automatically.
     *
     * @param chatId    Chat Room Id
     * @return          Flow of [ChatVideoUpdate]
     */
    fun getChatLocalVideoUpdates(chatId: Long): Flow<ChatVideoUpdate>

    /**
     * Open video device
     *
     * The associated request type with this request is MegaChatRequest::TYPE_OPEN_VIDEO_DEVICE
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getFlag - Returns true open device
     *
     * @param listener MegaChatRequestListener to track this request
     */
    fun openVideoDevice(listener: MegaChatRequestListenerInterface)

    /**
     * Release video device
     *
     * The associated request type with this request is MegaChatRequest::TYPE_OPEN_VIDEO_DEVICE
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getFlag - Returns false close device
     *
     * @param listener MegaChatRequestListener to track this request
     */
    fun releaseVideoDevice(listener: MegaChatRequestListenerInterface)

    /**
     * Push a list of users (for all it's connected clients) into the waiting room.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param userList MegaHandleList with the users that must be pushed into waiting room.
     * @param all if true indicates that all users with non moderator role, must be pushed into waiting room
     * @param listener MegaChatRequestListener to track this request
     */
    fun pushUsersIntoWaitingRoom(
        chatId: Long,
        userList: MegaHandleList?,
        all: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Disconnects all clients of the specified users, regardless of whether they are in the call or in the waiting room.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param userList MegaHandleList with the users that must be disconnected from call
     * @param listener MegaChatRequestListener to track this request
     */
    fun kickUsersFromCall(
        chatId: Long,
        userList: MegaHandleList?,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Allow a list of users in the waiting room to join the call.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param userList MegaHandleList with the users that must be allowed into waiting room.
     * @param all if true indicates that all users with non moderator role, must be pushed into waiting room
     * @param listener MegaChatRequestListener to track this request
     */
    fun allowUsersJoinCall(
        chatId: Long,
        userList: MegaHandleList?,
        all: Boolean,
        listener: MegaChatRequestListenerInterface,
    )

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
     * @param listener MegaChatRequestListener to track this request
     */
    fun attachNode(
        chatId: Long,
        nodeHandle: Long,
        listener: MegaChatRequestListenerInterface,
    )

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
     * @param listener MegaChatRequestListener to track this request
     */
    fun attachVoiceMessage(
        chatId: Long,
        nodeHandle: Long,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Returns true if there is a call at chatroom with id chatId.
     * Not it's not necessary that we participate in the call, but other participants do.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @return True if there is a call in a chatroom. False in other case
     */
    suspend fun hasCallInChatRoom(chatId: Long): Boolean

    /**
     * @brief Returns if audio level monitor is enabled
     *
     * It's false by default
     *
     * @note If there isn't a call in that chatroom in which user is participating,
     * audio Level monitor will be always false
     *
     * @param chatId MegaChatHandle that identifies the chat room from we want know if audio level monitor is disabled
     * @return true if audio level monitor is enabled
     */
    suspend fun isAudioLevelMonitorEnabled(chatId: Long): Boolean

    /**
     * Enable or disable audio level monitor.
     *
     * Audio level monitor detects when a peer starts or stops speaking, and triggers a callback
     * (onChatSessionUpdate with change type CHANGE_TYPE_AUDIO_LEVEL) to inform apps about that event.
     *
     * It's false by default and it's app responsibility to enable it
     *
     * @param enable True for enable audio level monitor, False to disable
     * @param chatId MegaChatHandle that identifies the chat room where we can enable audio level monitor
     */
    suspend fun enableAudioLevelMonitor(enable: Boolean, chatId: Long)

    /**
     * Request high resolution video from a client
     *
     * The associated request type with this request is MegaChatRequest::TYPE_REQUEST_HIGH_RES_VIDEO
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getFlag - true -> indicate that request high resolution video
     * - MegaChatRequest::getUserHandle - Returns the clientId of the user
     * - MegaChatRequest::getPrivilege - Returns MegaChatCall::CALL_QUALITY_HIGH_DEF
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param clientId MegaChatHandle that identifies client
     * @param listener MegaChatRequestListener to track this request
     */
    fun requestHiResVideo(
        chatId: Long,
        clientId: Long,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Stop high resolution video from a list of clients
     *
     * The associated request type with this request is MegaChatRequest::TYPE_REQUEST_HIGH_RES_VIDEO
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getFlag - false -> indicate that stop high resolution video
     * - MegaChatRequest::getMegaHandleList - Returns the list of clients Ids
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param clientIds List of clients Ids
     * @param listener MegaChatRequestListener to track this request
     */
    fun stopHiResVideo(
        chatId: Long,
        clientIds: MegaHandleList?,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Request low resolution video from a list of clients
     *
     * The associated request type with this request is MegaChatRequest::TYPE_REQUEST_LOW_RES_VIDEO
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getFlag - true -> indicate that request low resolution video
     * - MegaChatRequest::getMegaHandleList - Returns the list of client Ids
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param clientIds List of clients Ids
     * @param listener MegaChatRequestListener to track this request
     */
    fun requestLowResVideo(
        chatId: Long,
        clientIds: MegaHandleList?,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * Stop low resolution video from a list of clients
     *
     * The associated request type with this request is MegaChatRequest::TYPE_REQUEST_LOW_RES_VIDEO
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getFlag - false -> indicate that stop low resolution video
     * - MegaChatRequest::getMegaHandleList - Returns the list of clients Ids
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param clientIds List of clients Ids
     * @param listener MegaChatRequestListener to track this request
     */
    fun stopLowResVideo(
        chatId: Long,
        clientIds: MegaHandleList?,
        listener: MegaChatRequestListenerInterface,
    )

    /**
     * End a call in a chat room (user must be moderator)
     *
     * The associated request type with this request is MegaChatRequest::TYPE_HANG_CHAT_CALL
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the call identifier
     * - MegaChatRequest::getFlag - Returns true
     *
     * @param callId MegaChatHandle that identifies the chat room
     * @param listener MegaChatRequestListener to track this request
     */
    fun endChatCall(callId: Long, listener: MegaChatRequestListenerInterface)

    /**
     * Sends a new message to the specified chatroom
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
     * a message id set to INVALID_HANDLE.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param message Content of the message
     * application-specific type like link, share, picture etc.) @see MegaChatMessage::Type.
     *
     * @return MegaChatMessage that will be sent. The message id is not definitive, but temporal.
     */
    fun sendMessage(chatId: Long, message: String): MegaChatMessage

    /**
     * This method should be called when we want to close a public chat preview
     *
     * It automatically disconnect to this chat, remove all internal data related, and make
     * a cache cleanup in order to clean all the related records.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     */
    suspend fun closeChatPreview(chatId: Long)

    /**
     * Has url
     *
     * @param content
     * @return
     */
    fun hasUrl(content: String): Boolean

    /**
     * Share a geolocation in the specified chatroom
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
     * @param chatId MegaChatHandle that identifies the chat room
     * @param longitude from shared geolocation
     * @param latitude from shared geolocation
     * @param image Preview as a byte array encoded in Base64URL. It can be NULL
     * @return [MegaChatMessage] that will be sent. The message id is not definitive, but temporal.
     */
    fun sendGeolocation(
        chatId: Long,
        longitude: Float,
        latitude: Float,
        image: String,
    ): MegaChatMessage

    /**
     * Mute a specific client or all of them in a call
     * This method can be called only by users with moderator role
     * @param chatId Id that identifies the chat room
     * @param clientId Id that identifies the client we want to mute, or MEGACHAT_INVALID_HANDLE to mute all participants
     * @param listener MegaChatRequestListener to track this request
     */
    fun mutePeers(
        chatId: Long,
        clientId: Long,
        listener: MegaChatRequestListenerInterface,
    )

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
     * @param listener MegaChatRequestListener to track this request
     */
    fun addReaction(
        chatId: Long,
        msgId: Long,
        reaction: String,
        listener: MegaChatRequestListenerInterface,
    )
}
