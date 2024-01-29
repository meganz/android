package mega.privacy.android.app.presentation.time.mapper

import javax.inject.Inject
import kotlin.time.Duration

/**
 * Mapper to map seconds to a duration text. e.g. "01:23:45" or "23:45"
 *
 */
class DurationInSecondsTextMapper @Inject constructor() {
    /**
     * invoke
     *
     * @param duration [Duration]
     * @return duration text
     */
    operator fun invoke(duration: Duration?): String {
        val seconds = duration?.inWholeSeconds?.toInt() ?: 0
        val hours: Int = seconds / (60 * 60)
        val minutes: Int = (seconds % (60 * 60)) / (60)
        val finalSeconds: Int = seconds % 60
        return when {
            hours > 0 -> "%d:%02d:%02d".format(hours, minutes, finalSeconds)
            else -> "%d:%02d".format(minutes, finalSeconds)
        }
    }
}