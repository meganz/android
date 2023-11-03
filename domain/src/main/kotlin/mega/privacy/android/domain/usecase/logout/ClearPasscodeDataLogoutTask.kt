package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.passcode.SetPasscodeEnabledUseCase
import javax.inject.Inject

/**
 * Clear passcode data logout task
 *
 * @property passcodeRepository
 */
class ClearPasscodeDataLogoutTask @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
    private val setPasscodeEnabledUseCase: SetPasscodeEnabledUseCase,
) : LogoutTask {
    /**
     * Invoke
     *
     */
    override suspend fun invoke() {
        setPasscodeEnabledUseCase(false)
        passcodeRepository.setFailedAttempts(0)
    }
}