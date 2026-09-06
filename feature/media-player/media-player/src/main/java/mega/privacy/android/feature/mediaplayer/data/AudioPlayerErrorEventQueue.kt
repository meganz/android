package mega.privacy.android.feature.mediaplayer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A terminal playback error raised by the audio player service, to be announced to the user.
 */
sealed interface AudioPlayerError {

    /**
     * Playback kept failing and the retry budget is exhausted.
     *
     * @property isNetworkAvailable whether the device had a network connection when the retries
     * ran out; drives the wording of the error dialog
     */
    data class RetryExhausted(val isNetworkAvailable: Boolean) : AudioPlayerError

    /** The current node is blocked (taken down), so playback can never succeed. */
    data object NodeBlocked : AudioPlayerError

    /**
     * The play queue could not be built and nothing is loaded in the player.
     *
     * @property isNetworkAvailable whether the device had a network connection when the build
     * failed; drives the wording of the error dialog
     */
    data class QueueBuildFailed(val isNetworkAvailable: Boolean) : AudioPlayerError
}

/**
 * Holds the pending terminal playback error until the audio player UI shows the error dialog.
 *
 * The error is raised by the audio player service (in the app module, which this module cannot
 * reference), but the dialog belongs to the player screen, and nothing on the Media3 controller
 * path carries one-off events, so the service parks the error here and the ViewModel claims it —
 * the same handoff pattern as [AudioResumeEventQueue].
 */
@Singleton
class AudioPlayerErrorEventQueue @Inject constructor() {

    private val pending = MutableStateFlow<AudioPlayerError?>(null)

    /**
     * Emits while an error is waiting to be shown. Collectors must call [consume] to claim it.
     */
    val events: Flow<AudioPlayerError> = pending.filterNotNull()

    /**
     * Queue a terminal playback error.
     *
     * @param error the error that stopped playback
     */
    fun emit(error: AudioPlayerError) {
        pending.update { error }
    }

    /**
     * Claim the pending error, or null when it was already taken.
     */
    fun consume(): AudioPlayerError? = pending.getAndUpdate { null }

    /**
     * Discard any pending error. Called when new playback starts, which invalidates an error
     * the screen never showed.
     */
    fun clear() {
        pending.update { null }
    }
}
