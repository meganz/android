package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case for logging out of the MEGA account without invalidating the session.
 */
class LocalLogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
) {

    /**
     * Invoke.
     *
     * @param disableChatApi True to disable MegaChat API listener after chat logout.
     */
    suspend operator fun invoke(disableChatApi: Boolean) {
        chatLogoutUseCase(disableChatApi)
        runCatching { loginRepository.localLogout() }
            .onSuccess { localLogoutAppUseCase() }
    }
}
