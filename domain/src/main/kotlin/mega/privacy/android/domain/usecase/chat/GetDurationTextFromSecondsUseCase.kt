package mega.privacy.android.domain.usecase.chat

import javax.inject.Inject

/**
 * convert seconds to time string
 */
class GetDurationTextFromSecondsUseCase @Inject constructor() {

    /**
     * Invoke
     *
     * @param seconds
     * @return the time string
     */
    operator fun invoke(seconds: Long): String {
        val minutesString: String
        val secondsString: String
        var hoursString = ""
        val hours = (seconds / (60 * 60)).toInt()
        val minutes = (seconds % (60 * 60)).toInt() / 60
        val sec = (seconds % (60 * 60) % 60).toInt()
        minutesString = if (minutes < 10) "0$minutes" else "$minutes"
        secondsString = if (sec < 10) "0$sec" else "$sec"
        if (hours > 0) {
            hoursString = if (hours < 10) "0$hours:" else "$hours:"
        }
        return "$hoursString$minutesString:$secondsString"
    }


}