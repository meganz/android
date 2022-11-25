package mega.privacy.android.data.gateway.api

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.ScheduledMeetingUpdate
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatScheduledMeeting

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
    fun init(session: String): Int

    /**
     * Logouts API.
     */
    fun logout()

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
     * @param peers    MegaChatPeerList] including contacts and their privilege level.
     * @param listener Listener.
     */
    fun createChat(
        isGroup: Boolean,
        peers: MegaChatPeerList,
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
     * Gets a 1to1 chat conversation if exists.
     *
     * @param userHandle The user handle.
     * @return The chat conversation.
     */
    fun getChatRoomByUser(userHandle: Long): MegaChatRoom?

    /**
     * Returns the known alias given to the user.
     * Returns NULL if data is not cached yet or it's not possible to get.
     *
     * @param userHandle Handle of the user whose alias is requested.
     * @return The user alias.
     */
    fun getUserAliasFromCache(userHandle: Long): String?

    /**
     *  Returns the current full name of the user.
     *  Returns NULL if data is not cached yet or it's not possible to get.
     *
     * @param userHandle Handle of the user whose full name is requested.
     * @return The user full name.
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
     * @return The scheduled meeting.
     */
    fun getScheduledMeetingsByChat(chatId: Long): List<MegaChatScheduledMeeting>?

    /**
     * Get a list of all scheduled meeting occurrences for a chatroom
     *
     * @param chatId  MegaChatHandle that identifies a chat room
     * @param listener MegaChatRequestListener to track this request
     * @return The list of scheduled meetings occurrences.
     */
    fun fetchScheduledMeetingOccurrencesByChat(
        chatId: Long,
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
}
