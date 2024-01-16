package mega.privacy.android.app.presentation.meeting.chat.mapper

import javax.inject.Inject

/**
 * Mapper to map milli seconds to a duration text. e.g. "01:23:45" or "23:45"
 *
 */
class DurationTextMapper @Inject constructor() {
    /**
     * invoke
     *
     * @param milliSeconds
     * @return duration text
     */
    operator fun invoke(milliSeconds: Int): String {
        val hours: Int = (milliSeconds / (1000 * 60 * 60))
        val minutes: Int = (milliSeconds % (1000 * 60 * 60)) / (1000 * 60)
        val seconds: Int = (milliSeconds % (1000 * 60 * 60) % (1000 * 60) / 1000)
        return when {
            hours > 0 -> "%02d:%02d:%02d".format(hours, minutes, seconds)
            else -> "%02d:%02d".format(minutes, seconds)
        }
    }

}