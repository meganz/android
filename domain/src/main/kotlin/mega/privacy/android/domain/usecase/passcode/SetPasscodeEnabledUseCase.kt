package mega.privacy.android.domain.usecase.passcode

import mega.privacy.android.domain.exception.security.NoPasscodeSetException
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Set passcode enabled use case
 *
 * @property passcodeRepository
 */
class SetPasscodeEnabledUseCase @Inject constructor(private val passcodeRepository: PasscodeRepository) {
    /**
     * Invoke
     *
     * @param enabled
     */
    suspend operator fun invoke(enabled: Boolean) {
        if (enabled && passcodeRepository.getPasscode() == null) throw NoPasscodeSetException()
        if (!enabled) {
            passcodeRepository.setPasscode(null)
            passcodeRepository.setPasscodeType(null)
        }
        passcodeRepository.setPasscodeEnabled(enabled)
    }
}
