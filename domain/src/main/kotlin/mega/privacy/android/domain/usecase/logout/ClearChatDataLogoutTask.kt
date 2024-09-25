package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.chat.message.ClearAllChatDataUseCase
import javax.inject.Inject

/**
 * Clear chat data logout task
 *
 * @property clearAllChatDataUseCase
 */
class ClearChatDataLogoutTask @Inject constructor(
    private val clearAllChatDataUseCase: ClearAllChatDataUseCase,
) : LogoutTask {

    /**
     * Invoke
     */
    override suspend fun onLogoutSuccess() {
        clearAllChatDataUseCase()
    }
}