package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Get fake incoming call use case
 */
class GetFakeIncomingCallStateUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param chatId    Chat id
     * @return      [FakeIncomingCallState]
     */
    suspend operator fun invoke(chatId: Long): FakeIncomingCallState? =
        callRepository.getFakeIncomingCall(chatId = chatId)
}