package mega.privacy.android.data.mapper.security

import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import javax.inject.Inject

/**
 * Passcode timeout mapper
 */
internal class PasscodeTimeoutMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param timeOut
     */
    operator fun invoke(timeOut: Long?) = when (timeOut) {
        null, -1L -> null
        in (0..500L) -> PasscodeTimeout.Immediate
        else -> PasscodeTimeout.TimeSpan(timeOut)
    }
}
