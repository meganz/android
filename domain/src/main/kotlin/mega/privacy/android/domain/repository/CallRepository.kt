package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatSession
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
     * Get next available scheduled meeting occurrence given the current time
     *
     * @param chatId    MegaChatHandle that identifies a chat room
     * @return          ChatScheduledMeetingOccurr
     */
    suspend fun getNextScheduledMeetingOccurrence(chatId: Long): ChatScheduledMeetingOccurr?

    /**
     * Monitor chat call updates
     *
     * @return A flow of [ChatCall]
     */
    fun monitorChatCallUpdates(): Flow<ChatCall>

    /**
     * Monitor chat session updates
     *
     * @return A flow of [ChatSession]
     */
    fun monitorChatSessionUpdates(): Flow<ChatSession>

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
}