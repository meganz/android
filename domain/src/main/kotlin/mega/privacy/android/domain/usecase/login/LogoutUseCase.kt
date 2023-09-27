package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.logout.LogoutTask
import javax.inject.Inject

/**
 * LogoutUseCase use case
 */
class LogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val logoutTasks: Set<@JvmSuppressWildcards LogoutTask>,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() {
        loginRepository.setLogoutInProgressFlag(true)
        runCatching {
            logoutTasks.forEach {
                it()
            }
            loginRepository.logout()
        }.onFailure {
            loginRepository.setLogoutInProgressFlag(false)
            throw it
        }
    }
}
