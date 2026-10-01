package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case for logging out of the MEGA account locally without invalidating the server session.
 * Used when switching accounts in QA settings.
 */
class LocalLogoutChatAppUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    private val chatLocalLogoutUseCase: ChatLocalLogoutUseCase,
) {

    /**
     * Invoke.
     *
     * Errors are intentionally not caught here so the caller can set up its own
     * runCatching for easier debugging.
     *
     * @param disableChatApi True to disable MegaChat API listener after chat local logout.
     */
    suspend operator fun invoke(disableChatApi: Boolean) {
        chatLocalLogoutUseCase(disableChatApi)
        loginRepository.localLogout()
        localLogoutAppUseCase()
    }
}
