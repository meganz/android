package mega.privacy.android.app.presentation.meeting.chat.mapper

import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Mapper to map milli seconds to a duration text. e.g. "01:23:45" or "23:45"
 *
 */
class DurationTextMapper @Inject constructor() {
    /**
     * invoke
     *
     * @param duration [Duration]
     * @return duration text
     */
    operator fun invoke(duration: Duration?, unit: DurationUnit): String {
        when (unit) {
            DurationUnit.MILLISECONDS -> {
                val milliSeconds = duration?.inWholeMilliseconds?.toInt() ?: 0
                val hours: Int = (milliSeconds / (1000 * 60 * 60))
                val minutes: Int = (milliSeconds % (1000 * 60 * 60)) / (1000 * 60)
                val seconds: Int = (milliSeconds % (1000 * 60 * 60) % (1000 * 60) / 1000)
                return when {
                    hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
                    else -> "%d:%02d".format(minutes, seconds)
                }
            }

            DurationUnit.SECONDS -> {
                val seconds = duration?.inWholeSeconds?.toInt() ?: 0
                val hours: Int = seconds / (60 * 60)
                val minutes: Int = (seconds % (60 * 60)) / (60)
                val finalSeconds: Int = seconds % 60
                return when {
                    hours > 0 -> "%d:%02d:%02d".format(hours, minutes, finalSeconds)
                    else -> "%d:%02d".format(minutes, finalSeconds)
                }
            }

            else -> return ""
        }
    }

}