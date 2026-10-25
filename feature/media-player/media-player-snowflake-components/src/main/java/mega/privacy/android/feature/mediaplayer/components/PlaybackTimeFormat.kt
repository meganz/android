package mega.privacy.android.feature.mediaplayer.components

/**
 * Formats a playback time in milliseconds as "M:SS", or "H:MM:SS" from one hour.
 *
 * Negative and unset values, which the player reports before it knows the length of an item,
 * are shown as a zero time rather than as a blank or a negative one.
 */
fun formatPlaybackTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
