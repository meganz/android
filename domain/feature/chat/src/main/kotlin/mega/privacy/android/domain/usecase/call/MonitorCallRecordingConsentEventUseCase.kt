package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.call.CallRecordingConsentStatus
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
    operator fun invoke(): Flow<CallRecordingConsentStatus> =
        callRepository.monitorCallRecordingConsentEvent()
}