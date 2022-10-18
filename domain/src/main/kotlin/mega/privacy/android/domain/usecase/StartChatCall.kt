package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for start call in a chat
 */
fun interface StartChatCall {

    /**
     * Invoke.
     *
     * @param chatId  The chat id.
     * @param enabledVideo True for audio-video call, false for audio call
     * @param enabledAudio True for starting a call with audio (mute disabled)
     * @return The Chat Request.
     */
    suspend operator fun invoke(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest
}