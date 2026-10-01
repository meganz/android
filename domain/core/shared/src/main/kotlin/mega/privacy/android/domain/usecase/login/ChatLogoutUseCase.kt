package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case for logging out from chat api.
 */
class ChatLogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val disableChatApiUseCase: DisableChatApiUseCase,
) {

    /**
     * Invoke.
     *
     * @param disableChatApi True to disable MegaChat API listener after chat logout.
     */
    suspend operator fun invoke(disableChatApi: Boolean) {
        runCatching { loginRepository.chatLogout() }
            .onSuccess {
                if (disableChatApi) {
                    disableChatApiUseCase()
                }
            }
    }
}
