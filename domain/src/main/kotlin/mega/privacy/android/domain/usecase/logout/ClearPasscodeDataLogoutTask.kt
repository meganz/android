package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Clear passcode data logout task
 *
 * @property passcodeRepository
 */
class ClearPasscodeDataLogoutTask @Inject constructor(private val passcodeRepository: PasscodeRepository) :
    LogoutTask {
    /**
     * Invoke
     *
     */
    override suspend fun invoke() = passcodeRepository.setFailedAttempts(0)
}