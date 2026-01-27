package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

class InitAnonymousChatModeUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    suspend operator fun invoke() {
        if (chatRepository.getChatInitState() < ChatInitState.WAITING_NEW_SESSION) {
            if (chatRepository.initAnonymousChat() == ChatInitState.ERROR) {
                throw ChatNotInitializedErrorStatus()
            }
        }
    }
}