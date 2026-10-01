package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.logout.LogoutTask
import javax.inject.Inject

/**
 * LogoutUseCase use case
 */
class LogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val setLogoutInProgressFlagUseCase: SetLogoutInProgressFlagUseCase,
    private val logoutTasks: Set<@JvmSuppressWildcards LogoutTask>,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() {
        setLogoutInProgressFlagUseCase(true)
        runCatching {
            logoutTasks.forEach {
                it.onPreLogout()
            }
            loginRepository.logout()
        }.onSuccess {
            // Once the logout has succeeded, the cleanup tasks must always run, even if the
            // calling scope is cancelled (e.g. the ViewModel is cleared when the app navigates
            // to the login screen) or one of the tasks fails. The tasks are independent, so
            // they run concurrently, and we wait for all of them to finish or fail.
            withContext(NonCancellable) {
                logoutTasks.map { task ->
                    launch {
                        runCatching { task.onLogoutSuccess() }
                            .onFailure {
                                Log.e("Logout task ${task::class.simpleName} failed", it)
                            }
                    }
                }.joinAll()
            }
        }.onFailure {
            setLogoutInProgressFlagUseCase(false)
            logoutTasks.forEach { task ->
                task.onLogoutFailed(it)
            }
            throw it
        }
    }
}
