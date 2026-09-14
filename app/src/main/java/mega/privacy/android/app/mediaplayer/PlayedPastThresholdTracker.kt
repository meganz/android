package mega.privacy.android.app.mediaplayer

import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo

/**
 * Remembers which item was last played past [CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS] in the current
 * player session, so that a snapshot at or below the threshold can be told apart from the natural
 * start of an item.
 *
 * Only a below-threshold snapshot of the item that was played past the threshold means the user
 * returned to its start and may discard its stored position. For any other item such a snapshot
 * is produced by the player itself: the pause issued before the saved position is looked up on a
 * track change, or the buffering pause at a track start, both report position 0.
 *
 * One instance per player session: the audio service and each video player ViewModel track the
 * item they are playing themselves, and those items are never the same one, so the instances are
 * deliberately not shared. Not thread-safe: meant to be fed from the player callbacks of a single
 * session.
 */
internal class PlayedPastThresholdTracker {

    private var playedPastThresholdHandle: Long? = null

    /**
     * Record a snapshot of the item currently in the player.
     *
     * Moving to another item forgets the previous one, so returning to an item that was played
     * past the threshold earlier in the session starts afresh and cannot discard its position.
     */
    fun track(info: MediaPlaybackInfo) {
        playedPastThresholdHandle = when {
            info.currentPosition > CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS -> info.mediaHandle
            info.mediaHandle != playedPastThresholdHandle -> null
            else -> playedPastThresholdHandle
        }
    }

    /**
     * Whether [info] is a below-threshold snapshot of an item that was not played past the
     * threshold in this session, and must therefore not reach the save path.
     */
    fun isStartOfUnplayedItem(info: MediaPlaybackInfo): Boolean =
        info.currentPosition <= CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS &&
                info.mediaHandle != playedPastThresholdHandle
}
