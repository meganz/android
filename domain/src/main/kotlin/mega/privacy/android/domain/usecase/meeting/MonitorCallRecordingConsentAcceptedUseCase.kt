package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when the call recording consent has been accepted.
 */
class MonitorCallRecordingConsentAcceptedUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Invoke
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = callRepository.monitorCallRecordingConsentAccepted()
}