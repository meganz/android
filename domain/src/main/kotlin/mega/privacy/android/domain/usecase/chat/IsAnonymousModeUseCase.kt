package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for checking if it is in anonymous mode.
 */
class IsAnonymousModeUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     */
    suspend operator fun invoke() =
        chatRepository.getChatInitState() == ChatInitState.ANONYMOUS
}