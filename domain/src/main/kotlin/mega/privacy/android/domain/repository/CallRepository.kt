package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatCall

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
     * Monitor chat call updates
     *
     * @return A flow of [ChatCall]
     */
    fun monitorChatCallUpdates(): Flow<ChatCall>
}