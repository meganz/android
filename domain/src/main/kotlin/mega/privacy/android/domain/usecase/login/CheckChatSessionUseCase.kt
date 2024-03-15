package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.usecase.GetChatInitStateUseCase
import javax.inject.Inject

/**
 * Check chat session use case
 *
 */
class CheckChatSessionUseCase @Inject constructor(
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val getChatInitStateUseCase: GetChatInitStateUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() {
        val state = getChatInitStateUseCase()
        if (state == ChatInitState.NOT_DONE || state == ChatInitState.ERROR) {
            val session = getSessionUseCase() ?: throw IllegalStateException("Session not found")
            initialiseMegaChatUseCase(session)
        }
    }
}