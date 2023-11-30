package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when the call recording consent has been accepted or rejected.
 */
class MonitorCallRecordingConsentEventUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Invoke
     *
     * @return Flow of Boolean. True if consent has been accepted or False otherwise.
     */
    operator fun invoke() = callRepository.monitorCallRecordingConsentEvent()
}