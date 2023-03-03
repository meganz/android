package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for answers call in a chat
 */
fun interface AnswerChatCall {

    /**
     * Invoke.
     *
     * @param chatId  The chat id.
     * @param enabledVideo True for audio-video call, false for audio call
     * @param enabledAudio True for answering a call with audio (mute disabled)
     * @return The chat conversation handle.
     */
    suspend operator fun invoke(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest
}