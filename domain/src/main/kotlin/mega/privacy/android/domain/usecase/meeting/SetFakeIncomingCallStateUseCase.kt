package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Set fake incoming call use case
 */
class SetFakeIncomingCallStateUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param chatId    Chat id
     * @param type      [FakeIncomingCallState]
     */
    suspend operator fun invoke(chatId: Long, type: FakeIncomingCallState?) {
        when (type) {
            FakeIncomingCallState.Notification -> callRepository.addFakeIncomingCall(chatId, type)

            FakeIncomingCallState.Screen,
            FakeIncomingCallState.Dismiss,
            FakeIncomingCallState.Remove,
                -> if (callRepository.getFakeIncomingCall(chatId) != null) {
                callRepository.addFakeIncomingCall(chatId, type)
            }

            null -> callRepository.removeFakeIncomingCall(chatId)
        }
    }
}