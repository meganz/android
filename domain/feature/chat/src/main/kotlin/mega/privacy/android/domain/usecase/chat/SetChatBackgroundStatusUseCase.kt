package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Notify the chat engine whether the app is in background, skipping the call when the engine
 * status is unavailable or already matches.
 */
class SetChatBackgroundStatusUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     *
     * @param inBackground True when the app is in background
     */
    suspend operator fun invoke(inBackground: Boolean) {
        val isChatInBackground = chatRepository.isChatInBackground() ?: return
        if (isChatInBackground != inBackground) {
            chatRepository.setChatBackgroundStatus(inBackground)
        }
    }
}
