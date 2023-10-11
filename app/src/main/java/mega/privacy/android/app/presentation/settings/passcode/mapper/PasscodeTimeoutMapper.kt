package mega.privacy.android.app.presentation.settings.passcode.mapper

import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import javax.inject.Inject

/**
 * Passcode timeout mapper
 */
class PasscodeTimeoutMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param millisecondString
     * @return PasscodeTimeout
     */
    operator fun invoke(millisecondString: String): PasscodeTimeout? {
        return when (val timeSpan = millisecondString.toLongOrNull()) {
            null -> null
            0L -> PasscodeTimeout.Immediate
            else -> PasscodeTimeout.TimeSpan(timeSpan)
        }
    }
}