package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * UI data class for playlist screen.
 *
 * @property nodeHandle node handle
 * @property nodeName node name
 * @property thumbnail thumbnail file path, null if not available
 * @property index the index used for seek to this item
 * @property type item type
 * @property size size of the node
 * @property isSelected Whether the item is selected
 * @property headerIsVisible the header of item if is visible
 * @property duration the duration of audio
 * @property currentPosition the current playing position of audio
 */
data class PlaylistItem(
    val nodeHandle: Long,
    val nodeName: String,
    val thumbnail: File?,
    var index: Int,
    val type: Int,
    val size: Long,
    var isSelected: Boolean = false,
    var headerIsVisible: Boolean = false,
    var duration: Int = 0,
    var currentPosition: Long = 0L
) {
    /**
     * Create a new instance with the specified index and item type,
     * and nullify thumbnail if it's not exist.
     *
     * @param index new index
     * @param type item type
     * @param isSelected Whether the item is selected
     * @param duration the duration of audio
     * @param currentPosition the current playing position of audio
     * @return the new instance
     */
    fun finalizeItem(
        index: Int,
        type: Int,
        isSelected: Boolean = false,
        duration: Int = 0,
        currentPosition: Long = 0L
    ): PlaylistItem {
        return PlaylistItem(
            nodeHandle = nodeHandle,
            nodeName = nodeName,
            thumbnail = if (thumbnail?.exists() == true) thumbnail else null,
            index = index,
            type = type,
            size = size,
            isSelected = isSelected,
            duration = duration,
            currentPosition = currentPosition
        )
    }

    /**
     * Create a new instance with new node name.
     *
     * @param newName new node name
     * @return the new instance
     */
    fun updateNodeName(newName: String) =
        PlaylistItem(
            nodeHandle = nodeHandle,
            nodeName = newName,
            thumbnail = thumbnail,
            index = index,
            type = type,
            size = size,
            isSelected = isSelected
        )

    /**
     * Format current position and duration
     * @return strings of time
     */
    fun formatCurrentPositionAndDuration(currentPlayingPosition: Long) =
        "${formatMillisecondsToString(milliseconds = currentPlayingPosition)} / ${
            formatSecondsToString(seconds = duration)
        }"

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

    /**
     * Format duration
     * @return time strings
     */
    fun formatDuration() = formatSecondsToString(seconds = this.duration)

    companion object {
        /**
         * The previous type of media item
         */
        const val TYPE_PREVIOUS = 1

        /**
         * The playing type playing media item
         */
        const val TYPE_PLAYING = 2

        /**
         * The next type next media item
         */
        const val TYPE_NEXT = 3

        /**
         * Get name of item header
         * @param isAudio check if the item is audio
         * @param type item type
         * @param paused media whether is paused
         * @return the name of header
         */
        fun getHeaderName(isAudio: Boolean, type: Int, paused: Boolean = false): String {
            return getString(
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
        }
    }
}
