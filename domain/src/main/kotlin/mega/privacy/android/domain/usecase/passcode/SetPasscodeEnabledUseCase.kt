package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.exception.security.NoPasscodeSetException
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Set passcode enabled use case
 *
 * @property passcodeRepository
 */
class SetPasscodeEnabledUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     *
     * @param enabled
     */
    suspend operator fun invoke(enabled: Boolean) {
        if (enabled && noPasscodeSet()) throw NoPasscodeSetException()
        if (!enabled) {
            passcodeRepository.setPasscode(null)
            passcodeRepository.setPasscodeType(null)
            setDefaultTimeOut()
        } else if (noTimeOutSet()) {
            setDefaultTimeOut()
        }

        passcodeRepository.setPasscodeEnabled(enabled)
    }

    private suspend fun noPasscodeSet() = passcodeRepository.getPasscode() == null

    private suspend fun noTimeOutSet() = passcodeRepository.monitorPasscodeTimeOut()
        .firstOrNull() == null

    private suspend fun setDefaultTimeOut() {
        passcodeRepository.setPasscodeTimeOut(
            PasscodeTimeout.TimeSpan(30 * 1000)
        )
    }
}
