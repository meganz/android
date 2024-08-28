package mega.privacy.android.domain.usecase.passcode

import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Set passcode timeout use case
 *
 * @property passcodeRepository
 */
class SetPasscodeTimeoutUseCase @Inject constructor(private val passcodeRepository: PasscodeRepository) {
    /**
     * Invoke
     *
     * @param timeout
     */
    suspend operator fun invoke(timeout: PasscodeTimeout) =
        passcodeRepository.setPasscodeTimeOut(timeout)
}