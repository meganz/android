package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * A use case class to set the logout progress status.
 *
 * @property loginRepository [LoginRepository]
 */
class SetLogoutInProgressFlagUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
) {

    /**
     * Invocation method.
     *
     * @param isLoggingOut Whether the logout is in progress.
     */
    operator fun invoke(isLoggingOut: Boolean) =
        loginRepository.setLogoutInProgressFlag(isLoggingOut)
}
