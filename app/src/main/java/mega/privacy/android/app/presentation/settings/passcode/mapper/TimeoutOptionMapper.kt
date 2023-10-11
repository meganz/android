package mega.privacy.android.app.presentation.settings.passcode.mapper

import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import javax.inject.Inject

/**
 * Timeout option mapper
 */
class TimeoutOptionMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param timeout
     * @return associated timeout option
     */
    operator fun invoke(timeout: PasscodeTimeout): TimeoutOption {
        return when (timeout) {
            PasscodeTimeout.Immediate -> TimeoutOption.Immediate
            is PasscodeTimeout.TimeSpan -> timeoutOption(timeout.milliseconds)
        }
    }

    private fun timeoutOption(timeSpanMilliseconds: Long) =
        timeSpanMilliseconds.asSeconds().let { seconds ->
            if (seconds < 60) {
                TimeoutOption.SecondsTimeSpan(seconds)
            } else {
                TimeoutOption.MinutesTimeSpan(seconds / 60)
            }
        }

    private fun Long.asSeconds() = (this / 1000).toInt()
}