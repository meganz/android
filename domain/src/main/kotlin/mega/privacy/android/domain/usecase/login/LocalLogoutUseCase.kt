package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.ClearPsa
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
     * @param disableChatApiUseCase Temporary param for disabling megaChatApi.
     * @param clearPsa       Temporary param for clearing Psa.
     */
    suspend operator fun invoke(disableChatApiUseCase: DisableChatApiUseCase, clearPsa: ClearPsa) {
        chatLogoutUseCase(disableChatApiUseCase)
        runCatching { loginRepository.localLogout() }
            .onSuccess { localLogoutAppUseCase(clearPsa) }
    }
}