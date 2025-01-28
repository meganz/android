package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Monitor fake incoming call use case
 */
class MonitorFakeIncomingCallStateUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @return Flow of chatId and [FakeIncomingCallType]
     */
    operator fun invoke() = callRepository.monitorFakeIncomingCall()
}