package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case to reinitialize chat session for guest users
 */
class InitGuestChatSessionUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val chatRepository: ChatRepository,
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     *
     * @param initAnonymous     True to init Chat in anonymous mode, false otherwise.
     */
    suspend operator fun invoke(initAnonymous: Boolean = true) {
        if (!accountRepository.isUserLoggedIn()
            && chatRepository.getChatInitState() != ChatInitState.ANONYMOUS
        ) {
            logout()

            if (initAnonymous) {
                chatRepository.initAnonymousChat()
            } else {
                loginRepository.initMegaChat()
            }
        }
    }

    private suspend fun logout() {
        runCatching { loginRepository.chatLogout() }.getOrNull()
    }
}
