package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
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
     * Update open invite setting.
     *
     * @param chatId   The Chat id.
     * @return True if non-hosts are allowed to add participants, false otherwise.
     */
    suspend fun setOpenInvite(chatId: Long): Boolean

    /**
     * Starts call.
     *
     * @param chatId   The Chat id.
     * @param enabledVideo True for audio-video call, false for audio call
     * @param enabledAudio True for starting a call with audio (mute disabled)
     * @return The chat conversation handle.
     */
    suspend fun startChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest

    /**
     * Answers call.
     *
     * @param chatId   The Chat id.
     * @param enabledVideo True for audio-video call, false for audio call
     * @param enabledAudio True for answering a call with audio (mute disabled)
     * @param enabledSpeaker True speaker on. False speaker off.
     * @return The chat conversation handle.
     */
    suspend fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        enabledSpeaker: Boolean,
    ): ChatRequest


    /**
     * Get chat files folder id if it exists
     */
    suspend fun getChatFilesFolderId(): NodeId?

    /**
     * Monitor updates on chat room item update
     *
     * @param chatId    Chat Id.
     * @return          A flow of [ChatRoom]
     */
    fun monitorChatRoomUpdates(chatId: Long): Flow<ChatRoom>

    /**
     * Gets chat room if it exists
     *
     * @param chatId      Chat Id
     * @return [ChatRoom] containing the updated data.
     */
    fun getChatRoom(chatId: Long): ChatRoom?

    /**
     * Get a scheduled meeting given a chatId and a scheduled meeting id
     *
     * @param chatId  MegaChatHandle that identifies a chat room
     * @param schedId MegaChatHandle that identifies a scheduled meeting
     * @return The scheduled meeting.
     */
    fun getScheduledMeeting(chatId: Long, schedId: Long): ChatScheduledMeeting?

    /**
     * Get a list of all scheduled meeting for a chatroom
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @return The scheduled meeting.
     */
    fun getScheduledMeetingsByChat(chatId: Long): List<ChatScheduledMeeting>?
}