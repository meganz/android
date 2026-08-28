package mega.privacy.android.feature.mediaplayer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A silent resume applied by the audio player service, to be announced to the user.
 *
 * @property mediaHandle the resumed item's media handle. Load-bearing: the player screen matches
 * it against the item actually playing before announcing the resume, which is what keeps a track
 * change that bypasses AudioPlayerService.rebuildPlayQueue -- an auto-advance to the next item --
 * from announcing this event against the wrong track.
 * @property resumePositionMs the position in milliseconds playback resumed from
 */
data class AudioResumeEvent(
    val mediaHandle: Long,
    val resumePositionMs: Long,
)

/**
 * Holds the pending "playback silently resumed" event until the audio player UI shows the
 * resume snackbar.
 *
 * The resume position is applied by the service when it sets the play queue, but the snackbar
 * belongs to the player screen, and nothing on the Media3 controller path carries one-off
 * events, so the service parks the event here and the ViewModel claims it.
 */
@Singleton
class AudioResumeEventQueue @Inject constructor() {

    private val pending = MutableStateFlow<AudioResumeEvent?>(null)

    /**
     * Emits while an event is waiting to be shown. Collectors must call [consume] to claim it.
     */
    val events: Flow<AudioResumeEvent> = pending.filterNotNull()

    /**
     * Queue a resume event.
     *
     * @param event the resume that was applied
     */
    fun emit(event: AudioResumeEvent) {
        pending.update { event }
    }

    /**
     * Claim the pending event, or null when it was already taken.
     */
    fun consume(): AudioResumeEvent? = pending.getAndUpdate { null }
}
