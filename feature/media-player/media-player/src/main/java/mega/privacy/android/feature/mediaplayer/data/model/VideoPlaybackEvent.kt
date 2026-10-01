package mega.privacy.android.feature.mediaplayer.data.model

/**
 * A one-shot signal from the video player that must be acted on exactly once.
 *
 * Deliberately kept out of [VideoControllerState]: a state snapshot is replayed to every new
 * collector and re-emitted on unrelated changes, which would make a save run twice or a retry
 * counter advance on its own.
 */
sealed interface VideoPlaybackEvent {

    /**
     * The player jumped to a position that did not follow from normal playback, so the position
     * held for the current item is stale and has to be written out now.
     */
    data object PositionJumped : VideoPlaybackEvent

    /**
     * Playback left [mediaHandle] at [positionMs].
     *
     * Carries the position explicitly because by the time this arrives the player already reports
     * the position and duration of the item it moved to.
     */
    data class ItemLeft(val mediaHandle: Long, val positionMs: Long) : VideoPlaybackEvent

    /**
     * The player failed with [errorCode], a `PlaybackException.ERROR_CODE_*` value.
     *
     * Delivered once per failure so the retry counter advances once per failure.
     */
    data class Error(val errorCode: Int) : VideoPlaybackEvent
}
