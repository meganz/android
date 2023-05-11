package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Check password reminder use case
 *
 */
class CheckPasswordReminderUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
) {

    /**
     * Launches a request to check if should show Password reminder.
     *
     * @param atLogout True if the request is launched before logout action, false otherwise.
     * @return True/false if the request finished with success, error if not.
     */
    suspend operator fun invoke(atLogout: Boolean) =
        loginRepository.shouldShowPasswordReminderDialog(atLogout)
}