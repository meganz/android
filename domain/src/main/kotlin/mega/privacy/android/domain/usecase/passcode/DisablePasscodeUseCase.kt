package mega.privacy.android.domain.usecase.passcode

import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Set passcode enabled use case
 *
 * @property passcodeRepository
 */
class DisablePasscodeUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        passcodeRepository.setPasscodeEnabled(false)
        passcodeRepository.setPasscode(null)
        passcodeRepository.setPasscodeType(null)
        passcodeRepository.setPasscodeTimeOut(null)
    }

}
