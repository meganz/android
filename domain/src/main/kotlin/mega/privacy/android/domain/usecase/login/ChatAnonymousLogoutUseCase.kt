package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case for logging out from chat api.
 */
class ChatAnonymousLogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     */
    suspend operator fun invoke() {
        if (chatRepository.getChatInitState() == ChatInitState.ANONYMOUS) {
            loginRepository.chatLogout()
        }
    }
}