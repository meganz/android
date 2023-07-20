package mega.privacy.android.domain.usecase.passcode

import mega.privacy.android.domain.exception.security.NoPasscodeSetException
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Check passcode use case
 *
 * @property passcodeRepository
 */
class CheckPasscodeUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     *
     * @param passcode
     * @return true if passcode is correct, else false
     */
    suspend operator fun invoke(passcode: String): Boolean {
        val currentPasscode = passcodeRepository.getPasscode() ?: throw NoPasscodeSetException()
        return passcode == currentPasscode
    }
}
