package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import javax.inject.Inject

/**
 * Set fake incoming call use case
 */
class SetFakeIncomingCallStateUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val isChatStatusConnectedForCallUseCase: IsChatStatusConnectedForCallUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId    Chat id
     * @param type      [FakeIncomingCallState]
     */
    suspend operator fun invoke(chatId: Long, type: FakeIncomingCallState?) {
        when (type) {
            FakeIncomingCallState.Notification -> if (!isChatStatusConnectedForCallUseCase(chatId)) {
                callRepository.addFakeIncomingCall(chatId, type)
            }

            FakeIncomingCallState.Screen,
            FakeIncomingCallState.Dismiss,
            FakeIncomingCallState.Remove,
                -> {
                callRepository.getFakeIncomingCall(chatId)?.let {
                    if (it != type) {
                        callRepository.addFakeIncomingCall(chatId, type)
                    }
                }
            }

            null -> if (callRepository.isFakeIncomingCall(chatId)) {
                callRepository.removeFakeIncomingCall(chatId)
            }
        }
    }
}