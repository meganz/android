package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when a specific call is ended.
 */
class MonitorCallEndedUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Invoke
     *
     * @return Flow of Long. ID of the call.
     */
    operator fun invoke() = callRepository.monitorCallEnded()
}