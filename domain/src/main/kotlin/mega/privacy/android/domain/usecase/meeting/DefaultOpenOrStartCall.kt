package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Default get chat participants use case implementation.
 */
class DefaultOpenOrStartCall @Inject constructor(
    private val callRepository: CallRepository,
) : OpenOrStartCall {

    override suspend fun invoke(chatId: Long, video: Boolean, audio: Boolean): ChatCall? {
        callRepository.getChatCall(chatId)?.let { call ->
            return call
        }

        runCatching {
            callRepository.startCallRinging(
                chatId,
                video,
                audio
            )
        }.fold(
            onSuccess = { request -> return callRepository.getChatCall(request.chatHandle) },
            onFailure = { return null }
        )
    }
}