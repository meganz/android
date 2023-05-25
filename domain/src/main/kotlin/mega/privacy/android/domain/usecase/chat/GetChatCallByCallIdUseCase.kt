package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for getting a Chat Call given its Call Id
 */
class GetChatCallByCallIdUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke.
     *
     * @param callId    Call id.
     * @return          [ChatCall]
     */
    suspend operator fun invoke(callId: Long): ChatCall? =
        callRepository.getChatCallByCallId(callId)
}
