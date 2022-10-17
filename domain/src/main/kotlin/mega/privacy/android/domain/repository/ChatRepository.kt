package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatRequest

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
}