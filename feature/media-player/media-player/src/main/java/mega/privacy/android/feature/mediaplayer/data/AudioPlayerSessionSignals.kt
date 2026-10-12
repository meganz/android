package mega.privacy.android.feature.mediaplayer.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-wide signals the playback service publishes for screens that outlive a session.
 *
 * `AudioPlayerService` is the only writer. The mini player is the reader: it is composed for as
 * long as its host activity lives, so it sees sessions start and end beneath it and cannot learn
 * either from a MediaController of its own.
 */
@Singleton
class AudioPlayerSessionSignals @Inject constructor() {

    private val _isDismissed = MutableStateFlow(false)

    private val _sessionStarts = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * True once the notification has been swiped away, until playback resumes or moves on.
     *
     * The mini player needs it to decide what to draw while the player buffers: a dismissed
     * notification leaves playback parked, so the spinner would never resolve and the play button
     * is shown instead.
     */
    val isDismissed: StateFlow<Boolean> = _isDismissed.asStateFlow()

    /**
     * Emits every time playback becomes active.
     *
     * A MediaController does not survive the session it connected to, so a reader that outlived
     * the previous session reconnects on an emission. Buffering one keeps a signal that arrives
     * before the reader is collecting, and dropping the older of two says the same thing.
     */
    val sessionStarts: SharedFlow<Unit> = _sessionStarts.asSharedFlow()

    /**
     * Records that the notification has been swiped away, or that it is showing again.
     */
    fun setDismissed(dismissed: Boolean) {
        _isDismissed.value = dismissed
    }

    /**
     * Records that playback became active, so readers can reconnect to the session.
     */
    fun onSessionStarted() {
        _sessionStarts.tryEmit(Unit)
    }
}
