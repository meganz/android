package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.ResetSdkLogger
import javax.inject.Inject

/**
 * Use case for logging out from chat api.
 */
class ChatLogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val resetSdkLogger: ResetSdkLogger,
) {

    /**
     * Invoke.
     *
     * @param disableChatApiUseCase Temporary param for disabling megaChatApi.
     */
    suspend operator fun invoke(disableChatApiUseCase: DisableChatApiUseCase) {
        runCatching { loginRepository.chatLogout() }
            .onSuccess {
                disableChatApiUseCase()
                resetSdkLogger()
            }
    }
}