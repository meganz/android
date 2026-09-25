package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case to reinitialize chat session for guest users
 */
class InitGuestChatSessionUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param anonymousMode     True to init Chat in anonymous mode, false otherwise.
     */
    suspend operator fun invoke(anonymousMode: Boolean = true) {
        if (!anonymousMode) {
            logout()
            loginRepository.initMegaChat()
        } else if (chatRepository.getChatInitState() != ChatInitState.ANONYMOUS) {
            logout()
            chatRepository.initAnonymousChat()
        }
    }

    private suspend fun logout() {
        runCatching { loginRepository.chatLogout() }.getOrNull()
    }
}
