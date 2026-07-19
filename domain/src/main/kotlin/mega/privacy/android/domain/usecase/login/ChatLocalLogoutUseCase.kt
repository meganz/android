package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case for logging out from chat api locally without invalidating the server session.
 */
class ChatLocalLogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val disableChatApiUseCase: DisableChatApiUseCase,
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
        loginRepository.chatLocalLogout()
        if (disableChatApi) {
            disableChatApiUseCase()
        }
    }
}
