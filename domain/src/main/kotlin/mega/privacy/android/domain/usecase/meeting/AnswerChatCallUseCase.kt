package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to answer chat call
 */
class AnswerChatCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId  The chat id.
     * @param video True for audio-video call, false for audio call
     * @param audio True for answering a call with audio (mute disabled)
     * @return [ChatCall]
     */
    suspend operator fun invoke(chatId: Long, video: Boolean, audio: Boolean): ChatCall? =
        when (chatId) {
            -1L -> error("Invalid Chat Id")
            else -> {
                val chatCallRequest = callRepository.answerChatCall(
                    chatId,
                    video,
                    audio
                )

                callRepository.getChatCall(chatCallRequest.chatHandle)
            }
        }
}
