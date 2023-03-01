package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Default start chat call no ringing use case implementation.
 */
class DefaultStartChatCallNoRinging @Inject constructor(
    private val callRepository: CallRepository,
) : StartChatCallNoRinging {
    override suspend fun invoke(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatCall? = runCatching {
        callRepository.startCallNoRinging(
            chatId,
            schedId,
            enabledVideo,
            enabledAudio
        )
    }.fold(
        onSuccess = { request -> callRepository.getChatCall(request.chatHandle) },
        onFailure = { null }
    )
}