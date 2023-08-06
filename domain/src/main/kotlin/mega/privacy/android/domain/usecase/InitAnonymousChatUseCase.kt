package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to init chat API as anonymous user
 */
class InitAnonymousChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Invoke
     * @return chat init state
     */
    suspend operator fun invoke() = chatRepository.getChatInitState()
}