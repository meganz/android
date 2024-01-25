package mega.privacy.android.app.mediaplayer.playlist

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter.Companion.TYPE_PREVIOUS
import mega.privacy.android.app.presentation.meeting.chat.mapper.DurationTextMapper
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Create a new instance with the specified index and item type,
 * and nullify thumbnail if it's not exist.
 *
 * @param index new index
 * @param type item type
 * @param isSelected Whether the item is selected
 * @param duration the duration of audio
 * @param headerIsVisible the header of item whether is visible.
 * @return the new instance
 */
fun PlaylistItem.finalizeItem(
    index: Int = this.index,
    type: Int,
    isSelected: Boolean = false,
    duration: Duration = 0.seconds,
    headerIsVisible: Boolean = false,
): PlaylistItem = copy(
    index = index,
    type = type,
    isSelected = isSelected,
    duration = duration,
    headerIsVisible = headerIsVisible
)

/**
 * Create a new instance with new node name.
 *
 * @param newName new node name
 * @return the new instance
 */
fun PlaylistItem.updateNodeName(newName: String) = copy(nodeName = newName)

/**
 * Format current position and duration
 *
 * @param currentPosition the current position of playing item
 * @return strings of time
 */
fun PlaylistItem.formatCurrentPositionAndDuration(
    currentPosition: Long,
    durationTextMapper: DurationTextMapper,
) = "${
    durationTextMapper(
        currentPosition.seconds,
        DurationUnit.SECONDS
    )
} / ${
    durationTextMapper(duration, DurationUnit.SECONDS)
}"

/**
 * Get name of item header
 * @param isAudio check if the item is audio
 * @param paused media whether is paused
 * @return the name of header
 */
fun PlaylistItem.getHeaderName(
    isAudio: Boolean,
    paused: Boolean = false,
    context: Context,
): String =
    context.getString(
        when (type) {
            TYPE_PREVIOUS -> if (isAudio) {
                R.string.media_player_audio_playlist_previous
            } else {
                R.string.media_player_video_playlist_previous
            }

            else -> {
                if (paused) {
                    R.string.audio_player_now_playing_paused
                } else {
                    R.string.audio_player_now_playing
                }
            }
        }
    )