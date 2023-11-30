package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when the call recording consent has been accepted or rejected.
 */
class BroadcastCallRecordingConsentEventUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param isRecordingConsentAccepted True if recording consent has been accepted or False otherwise.
     */
    suspend operator fun invoke(isRecordingConsentAccepted: Boolean) =
        callRepository.broadcastCallRecordingConsentEvent(isRecordingConsentAccepted)
}