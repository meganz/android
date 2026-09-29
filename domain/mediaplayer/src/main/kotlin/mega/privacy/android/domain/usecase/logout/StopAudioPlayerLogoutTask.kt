package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.StopAudioService
import javax.inject.Inject

/**
 * Stop audio player logout task
 */
class StopAudioPlayerLogoutTask @Inject constructor(
    private val stopAudioService: StopAudioService,
) : LogoutTask {

    /**
     * Stops the audio player once the logout has succeeded.
     *
     * The local logout cleanup also stops the player, but only as one step of a longer sequence.
     * Running it as its own task keeps it independent of the rest of that sequence, so a player
     * that outlives the session cannot carry over into the next account. It covers both player
     * implementations, so it does not need to know which one the feature flag selected.
     *
     * A failure is left to propagate on purpose: the runner gives every task its own coroutine
     * and catches per task, so failing here cannot strand the other cleanups.
     */
    override suspend fun onLogoutSuccess() {
        stopAudioService()
    }
}
