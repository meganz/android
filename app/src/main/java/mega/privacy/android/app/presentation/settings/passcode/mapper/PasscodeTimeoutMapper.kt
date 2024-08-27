package mega.privacy.android.app.presentation.settings.passcode.mapper

import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import javax.inject.Inject

/**
 * Passcode timeout mapper
 */
class PasscodeTimeoutMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param timeoutOption
     * @return PasscodeTimeout
     */
    operator fun invoke(timeoutOption: TimeoutOption): PasscodeTimeout {
        return when (val timeSpan = timeoutOption.getTimeoutInMilliseconds()) {
            0L -> PasscodeTimeout.Immediate
            else -> PasscodeTimeout.TimeSpan(timeSpan)
        }
    }
}