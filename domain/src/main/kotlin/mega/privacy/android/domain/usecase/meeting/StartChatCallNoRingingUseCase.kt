package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Start chat call no ringing use case implementation.
 */
class StartChatCallNoRingingUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val isChatStatusConnectedForCall: IsChatStatusConnectedForCallUseCase,
) {

    /**
     * Start a call without ringing the rest of the users (necessary for scheduled meetings)
     *
     * @param chatId        Chat Id.
     * @param schedId       Scheduled meeting Id.
     * @param enabledVideo  True for audio-video call, false for audio call.
     * @param enabledAudio  True for starting a call with audio (mute disabled).
     * @return              [ChatCall]
     */
    suspend operator fun invoke(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatCall? =
        when {
            chatId == -1L -> error("Invalid Chat Id")
            !isChatStatusConnectedForCall(chatId) -> error("Chat is not connected")
            else -> {
                val chatCallRequest = callRepository.startCallNoRinging(
                    chatId,
                    schedId,
                    enabledVideo,
                    enabledAudio
                )

                callRepository.getChatCall(chatCallRequest.chatHandle)
            }
        }
}
