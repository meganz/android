package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Start a call, without ringing the rest of the users, use case
 */
fun interface StartChatCallNoRinging {
    /**
     * Start a call without ringing the rest of the users (necessary for scheduled meetings)
     *
     * @param chatId        Chat Id.
     * @param schedId       Scheduled meeting Id.
     * @param enabledVideo  True for audio-video call, false for audio call.
     * @param enabledAudio  True for starting a call with audio (mute disabled).
     * @return              [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest
}