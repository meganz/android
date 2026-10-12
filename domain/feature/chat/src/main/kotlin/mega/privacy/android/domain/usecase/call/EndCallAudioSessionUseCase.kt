package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.repository.CallAudioRouteRepository
import javax.inject.Inject

/**
 * Stops routing call audio.
 */
class EndCallAudioSessionUseCase @Inject constructor(
    private val callAudioRouteRepository: CallAudioRouteRepository,
) {

    /**
     * Ends the call audio session and restores the audio state from before the session started.
     * Does nothing when no session is active.
     */
    suspend operator fun invoke() = callAudioRouteRepository.endSession()
}
