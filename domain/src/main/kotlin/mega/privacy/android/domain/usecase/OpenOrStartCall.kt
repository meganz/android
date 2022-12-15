package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.ChatCall

/**
 * Use case for start call in a chat
 */
fun interface OpenOrStartCall {

    /**
     * Invoke.
     *
     * @param chatId  The chat id.
     * @param video True for audio-video call, false for audio call
     * @param audio True for starting a call with audio (mute disabled)
     * @return [ChatCall]
     */
    suspend operator fun invoke(
        chatId: Long,
        video: Boolean,
        audio: Boolean,
        ): ChatCall?
}