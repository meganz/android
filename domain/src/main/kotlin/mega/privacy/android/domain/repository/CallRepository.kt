package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSessionUpdatesResult
import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.entity.meeting.ResultOccurrenceUpdate

/**
 * The repository interface regarding Chat calls.
 */
interface CallRepository {

    /**
     * Gets chat call if it exists
     *
     * @param chatId    Chat Id
     * @return          [ChatCall]
     */
    suspend fun getChatCall(chatId: Long?): ChatCall?

    /**
     * Get chat call by call id
     *
     * @param callId    Call Id
     * @return          [ChatCall]
     */
    suspend fun getChatCallByCallId(callId: Long): ChatCall?

    /**
     * Open call or start call and open it
     *
     * @param chatId        Chat Id
     * @param enabledVideo  True for audio-video call, false for audio call
     * @param enabledAudio  True for starting a call with audio (mute disabled)
     * @return              [ChatRequest]
     */
    suspend fun startCallRinging(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest


    /**
     * Open call or start scheduled meeting and open it
     *
     * @param chatId                Chat Id.
     * @param schedId               Scheduled meeting Id.
     * @param enabledVideo          True for audio-video call, false for audio call.
     * @param enabledAudio          True for starting a call with audio (mute disabled).
     * @return                      [ChatRequest]
     */
    suspend fun startCallNoRinging(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest

    /**
     * Start meeting with waiting room enabled.
     *
     * @param chatId        The chat id.
     * @param schedIdWr     The scheduled meeting id.
     * @param enabledVideo  True for audio-video call, false for audio call.
     * @param enabledAudio  True for starting a call with audio (mute disabled).
     * @return              [ChatRequest]
     */
    suspend fun startMeetingInWaitingRoomChat(
        chatId: Long,
        schedIdWr: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest

    /**
     * Ring a participant in chatroom with an ongoing call that they didn't pick up
     *
     * @param chatId        The chat ID
     * @param userId        The participant user ID
     * @param ringTimeout   Timeout in seconds (greater than 0) for the call to stop ringing
     * @return              [ChatRequest]
     */
    suspend fun ringIndividualInACall(
        chatId: Long,
        userId: Long,
        ringTimeout: Int = 40,
    ): ChatRequest

    /**
     * Answers call.
     *
     * @param chatId            The Chat id.
     * @param enabledVideo      True for audio-video call, false for audio call
     * @param enabledAudio      True for answering a call with audio (mute disabled)
     * @return                  [ChatRequest]
     */
    suspend fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest

    /**
     * Hang a call.
     *
     * @param callId            The Call id.
     * @return                  [ChatRequest]
     */
    suspend fun hangChatCall(
        callId: Long,
    ): ChatRequest

    /**
     * Hold chat call
     *
     * @param chatId        Chat Id
     * @param setOnHold     Flag to set call on hold
     * @return              [ChatRequest]
     */
    suspend fun holdChatCall(
        chatId: Long,
        setOnHold: Boolean,
    ): ChatRequest

    /**
     * Get all scheduled meetings
     *
     * @return List of scheduled meetings
     */
    suspend fun getAllScheduledMeetings(): List<ChatScheduledMeeting>?

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
     * Creates a chatroom and a scheduled meeting for that chatroom
     *
     * @param peerList List of peers
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
     * @return                  [ChatRequest]
     */
    suspend fun createChatroomAndSchedMeeting(
        peerList: List<Long>,
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
        flags: ChatScheduledFlags?,
        rules: ChatScheduledRules?,
        attributes: String?,
    ): ChatRequest

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
    suspend fun updateScheduledMeeting(
        chatId: Long,
        schedId: Long,
        timezone: String,
        startDate: Long,
        endDate: Long,
        title: String,
        description: String,
        cancelled: Boolean,
        flags: ChatScheduledFlags?,
        rules: ChatScheduledRules?,
        updateChatTitle: Boolean
    ): ChatRequest

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
    suspend fun updateScheduledMeetingOccurrence(
        chatId: Long,
        schedId: Long,
        overrides: Long,
        newStartDate: Long,
        newEndDate: Long,
        cancelled: Boolean,
    ): ChatRequest

    /**
     * Monitor chat call updates
     *
     * @return A flow of [ChatCall]
     */
    fun monitorChatCallUpdates(): Flow<ChatCall>

    /**
     * Monitor chat session updates
     *
     * @return A flow of [ChatSessionUpdatesResult]
     */
    fun monitorChatSessionUpdates(): Flow<ChatSessionUpdatesResult>

    /**
     * Monitor updates on scheduled meetings
     *
     * @return          A flow of [ChatScheduledMeeting]
     */
    fun monitorScheduledMeetingUpdates(): Flow<ChatScheduledMeeting>

    /**
     * Monitor updates on scheduled meeting occurrences
     *
     * @return          A flow of ResultOccurrenceUpdate
     */
    fun monitorScheduledMeetingOccurrencesUpdates(): Flow<ResultOccurrenceUpdate>

    /**
     * Get a list with the ids of chat-rooms where there are active calls
     *
     * @param state handle list will be fetched with the calls having this state
     *              Instance of [ChatCallStatus]
     * @return List [Long] of call handles which user is participating
     *         empty list is output is null or throws any errors
     *         returns all calls regardless their state if state is [ChatCallStatus.Unknown]
     */
    suspend fun getCallHandleList(state: ChatCallStatus): List<Long>

    /**
     * Get a list with the ids of active calls
     *
     * @return A list of ids of active calls
     */
    suspend fun getChatCallIds(): List<Long>

    /**
     * Monitor scheduled meeting canceled.
     *
     * @return Flow [Int]
     */
    fun monitorScheduledMeetingCanceled(): Flow<Int>

    /**
     * Broadcast scheduled meeting canceled.
     *
     * @param messageResId [Int]
     */
    suspend fun broadcastScheduledMeetingCanceled(messageResId: Int)

    /**
     * Select the video device to be used in calls.
     *
     * @param device Identifier of device to be selected.
     */
    suspend fun setChatVideoInDevice(device: String)

    /**
     * Get video updates from local device for an specific chat room.
     *
     * @param chatId    Chat Room Id
     * @return          Flow of [ChatVideoUpdate]
     */
    fun getChatLocalVideoUpdates(chatId: Long): Flow<ChatVideoUpdate>

    /**
     * Register a listener to receive video from remote device for an specific chat room.
     * This listener will be deregistered automatically.
     *
     * @param chatId    Chat Room Id
     * @param clientId  Client Id
     * @param hiRes    True if high resolution video is requested, false otherwise
     * @return          Flow of [ChatVideoUpdate]
     */
    fun getChatRemoteVideoUpdates(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
    ): Flow<ChatVideoUpdate>

    /**
     * Open video device
     *
     * @return      True if the device is opened, false otherwise.
     */
    suspend fun openVideoDevice(): ChatRequest

    /**
     * Release video device
     *
     * @return      True if the device is closed, false otherwise.
     */
    suspend fun releaseVideoDevice(): ChatRequest

    /**
     * Enable video
     *
     * @param chatId  Chat id
     * @return                  [ChatRequest]
     */
    suspend fun enableVideo(
        chatId: Long,
    ): ChatRequest

    /**
     * Disable video
     *
     * @param chatId  Chat id
     * @return                  [ChatRequest]
     */
    suspend fun disableVideo(
        chatId: Long,
    ): ChatRequest

    /**
     * Enable Audio
     *
     * @param chatId  Chat id
     * @return                  [ChatRequest]
     */
    suspend fun enableAudio(
        chatId: Long,
    ): ChatRequest

    /**
     * Disable Audio
     *
     * @param chatId  Chat id
     * @return                  [ChatRequest]
     */
    suspend fun disableAudio(
        chatId: Long,
    ): ChatRequest

    /**
     * Push a list of users (for all it's connected clients) into the waiting room.
     *
     * @param chatId  Chat id
     * @param userList List of users that must be pushed into waiting room.
     * @param all if true indicates that all users with non moderator role, must be pushed into waiting room
     * @return                  [ChatRequest]
     */
    suspend fun pushUsersIntoWaitingRoom(
        chatId: Long,
        userList: List<Long>,
        all: Boolean,
    ): ChatRequest

    /**
     * Disconnects all clients of the specified users, regardless of whether they are in the call or in the waiting room.
     *
     * @param chatId Chat id
     * @param userList List of users that must be pushed into waiting room.
     */
    suspend fun kickUsersFromCall(
        chatId: Long,
        userList: List<Long>,
    ): ChatRequest

    /**
     * Allow a list of users in the waiting room to join the call.
     *
     * @param chatId Chat id
     * @param userList List of users that must be pushed into waiting room.
     * @param all if true indicates that all users with non moderator role, must be pushed into waiting room
     */
    suspend fun allowUsersJoinCall(
        chatId: Long,
        userList: List<Long>,
        all: Boolean,
    ): ChatRequest

    /**
     * Raise hand to speak in a call
     *
     * @param chatId Chat id
     */
    suspend fun raiseHandToSpeak(
        chatId: Long,
    ): ChatRequest

    /**
     * Lowe hand to stop speak in a call
     *
     * @param chatId Chat id
     */
    suspend fun lowerHandToStopSpeak(
        chatId: Long,
    ): ChatRequest

    /**
     * Request high resolution video from a client
     *
     * @param chatId Chat id
     * @param clientId Client id
     */
    suspend fun requestHiResVideo(
        chatId: Long,
        clientId: Long,
    ): ChatRequest

    /**
     * Stop high resolution video from a list of clients
     *
     * @param chatId Chat id
     * @param clientIds List of client ids
     */
    suspend fun stopHiResVideo(
        chatId: Long,
        clientIds: List<Long>,
    ): ChatRequest

    /**
     * Request low resolution video from a list of clients
     *
     * @param chatId Chat id
     * @param clientIds List of client ids
     */
    suspend fun requestLowResVideo(
        chatId: Long,
        clientIds: List<Long>,
    ): ChatRequest

    /**
     * Stop low resolution video from a list of clients
     *
     * @param chatId Chat id
     * @param clientIds List of client ids
     */
    suspend fun stopLowResVideo(
        chatId: Long,
        clientIds: List<Long>,
    ): ChatRequest

    /**
     * Monitor call recording consent event (accepted/rejected).
     *
     * @return StateFlow of Boolean. True if consent has been accepted or False otherwise.
     */
    fun monitorCallRecordingConsentEvent(): StateFlow<Boolean?>

    /**
     * Broadcast call recording consent event (accepted/rejected).
     *
     * @param isRecordingConsentAccepted True if recording consent has been accepted or False otherwise.
     */
    suspend fun broadcastCallRecordingConsentEvent(isRecordingConsentAccepted: Boolean?)

    /**
     * Monitor that a specific call has ended.
     *
     * @return Flow of Boolean. ID of the chat to which the call corresponded.
     */
    fun monitorCallEnded(): Flow<Long>

    /**
     * Broadcast that a specific call has ended.
     *
     * @param chatId    ID of the chat to which the call corresponded.
     */
    suspend fun broadcastCallEnded(chatId: Long)

    /**
     * Monitor that a specific call has ended.
     *
     * @return Flow of Boolean. ID of the call.
     */
    fun monitorCallScreenOpened(): Flow<Boolean>

    /**
     * Broadcast that a specific call has ended.
     *
     * @param isOpened  True, the call is opened. False, if the call is not opened.
     */
    suspend fun broadcastCallScreenOpened(isOpened: Boolean)

    /**
     * Broadcast that audio output has changed.
     *
     * @param audioDevice  [AudioDevice]
     */
    suspend fun broadcastAudioOutput(audioDevice: AudioDevice)

    /**
     * Monitor that audio output has changed.
     *
     * @return Flow of AudioDevice.
     */
    fun monitorAudioOutput(): Flow<AudioDevice>

    /**
     * Monitor if waiting for other participants has ended.
     *
     * @return Flow [Boolean]
     */
    fun monitorWaitingForOtherParticipantsHasEnded(): Flow<Pair<Long, Boolean>>

    /**
     * Broadcast if waiting for other participants has ended.
     *
     * @param isEnded  True, is ended. False if not.
     */
    suspend fun broadcastWaitingForOtherParticipantsHasEnded(chatId: Long, isEnded: Boolean)

    /**
     * Broadcast that local video has changed due to proximity sensor.
     *
     * @param isVideoOn  True, if video is on, false if it's off.
     */
    suspend fun broadcastLocalVideoChangedDueToProximitySensor(isVideoOn: Boolean)

    /**
     * Monitor that local video has changed due to proximity sensor.
     *
     * @return Flow of Boolean.
     */
    fun monitorLocalVideoChangedDueToProximitySensor(): Flow<Boolean>

    /**
     * Mute peers
     *
     * @param chatId            The chat id.
     * @param clientId          The client id.
     * @return                  [ChatRequest]
     */
    suspend fun mutePeers(
        chatId: Long,
        clientId: Long,
    ): ChatRequest

    /**
     * Mute all peers
     *
     * @param chatId            The chat id.
     * @return                  [ChatRequest]
     */
    suspend fun muteAllPeers(
        chatId: Long,
    ): ChatRequest

    /**
     * Ignore call
     *
     * @param chatId Chat id
     */
    suspend fun setIgnoredCall(
        chatId: Long,
    ): Boolean

    /**
     * Create meeting
     *
     * @param title         Meeting title
     * @param speakRequest  Speak request enable
     * @param waitingRoom   Waiting room enable
     * @param openInvite    Open invite enable
     * @return                 [ChatRequest]
     */
    suspend fun createMeeting(
        title: String,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
    ): ChatRequest

    /**
     * Monitor fake incoming call
     *
     * @return Flow of Map.
     */
    fun monitorFakeIncomingCall(): Flow<Map<Long, FakeIncomingCallState>>

    /**
     * Add fake incoming call
     *
     * @param chatId    Chat id
     * @param type      [FakeIncomingCallState]
     */
    suspend fun addFakeIncomingCall(chatId: Long, type: FakeIncomingCallState)

    /**
     * Remove fake incoming call
     *
     * @param chatId    Chat id
     */
    suspend fun removeFakeIncomingCall(chatId: Long)

    /**
     * Get fake incoming call
     *
     * @param chatId    Chat id
     * @return  [FakeIncomingCallState]
     */
    suspend fun getFakeIncomingCall(chatId: Long): FakeIncomingCallState?

    /**
     * Check if call is fake incoming call
     *
     * @param chatId    Chat id
     */
    suspend fun isFakeIncomingCall(chatId: Long): Boolean

    /**
     * Check if call is pending to hang up
     *
     * @param chatId    Chat id
     */
    suspend fun isPendingToHangUp(chatId: Long): Boolean

    /**
     * Add call pending to hang up
     *
     * @param chatId    Chat id
     */
    suspend fun addCallPendingToHangUp(chatId: Long)

    /**
     * Remove call pending to hang up
     *
     * @param chatId    Chat id
     */
    suspend fun removeCallPendingToHangUp(chatId: Long)
}
