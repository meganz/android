package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.passcode.DisablePasscodeUseCase
import javax.inject.Inject

/**
 * Clear passcode data logout task
 *
 * @property passcodeRepository
 */
class ClearPasscodeDataLogoutTask @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
    private val disablePasscodeUseCase: DisablePasscodeUseCase,
) : LogoutTask {
    /**
     * Invoke
     *
     */
    override suspend fun invoke() {
        disablePasscodeUseCase()
        passcodeRepository.setFailedAttempts(0)
    }
}