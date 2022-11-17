package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceViewModel.Companion.TYPE_PREVIOUS
import mega.privacy.android.app.utils.StringResourcesUtils
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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
    duration: Int = 0,
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
fun PlaylistItem.formatCurrentPositionAndDuration(currentPosition: Long) =
    "${formatMillisecondsToString(milliseconds = currentPosition)} / ${
        formatSecondsToString(seconds = duration)
    }"

/**
 * Format duration
 * @return time strings
 */
fun PlaylistItem.formatDuration() = formatSecondsToString(seconds = duration)

/**
 * Get name of item header
 * @param isAudio check if the item is audio
 * @param paused media whether is paused
 * @return the name of header
 */
fun PlaylistItem.getHeaderName(isAudio: Boolean, paused: Boolean = false): String =
    StringResourcesUtils.getString(
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


/**
 * Format milliseconds to time string
 * @param milliseconds time value that unit is milliseconds
 * @return strings of time
 */
private fun formatMillisecondsToString(milliseconds: Long): String {
    // Make the time displayed is same as Exoplayer
    val totalSeconds = (milliseconds.toFloat() / 1000).roundToInt()
    return formatSecondsToString(seconds = totalSeconds)
}

/**
 * Format seconds to time string
 * @param seconds time value that unit is seconds
 * @return strings of time
 */
private fun formatSecondsToString(seconds: Int): String {
    val hour = TimeUnit.SECONDS.toHours(seconds.toLong())
    val minutes =
        TimeUnit.SECONDS.toMinutes(seconds.toLong()) - TimeUnit.HOURS.toMinutes(hour)
    val resultSeconds =
        seconds.toLong() - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(
            seconds.toLong()))

    return if (hour >= 1) {
        String.format("%2d:%02d:%02d", hour, minutes, resultSeconds)
    } else {
        String.format("%02d:%02d", minutes, resultSeconds)
    }
}