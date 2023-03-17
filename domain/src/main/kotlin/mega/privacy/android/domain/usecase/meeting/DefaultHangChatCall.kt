package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Default implementation of hang call use case
 */
class DefaultHangChatCall @Inject constructor(
    private val callRepository: CallRepository,
) : HangChatCall {

    override suspend fun invoke(callId: Long): ChatCall? {
        if (callId == -1L) return null

        return runCatching {
            callRepository.hangChatCall(callId)
        }.fold(onSuccess = { request -> callRepository.getChatCall(request.chatHandle) },
            onFailure = { null })
    }
}