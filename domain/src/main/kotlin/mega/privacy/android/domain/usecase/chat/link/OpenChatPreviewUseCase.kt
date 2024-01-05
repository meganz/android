package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Load chat preview
 *
 * @property chatRepository
 */
class OpenChatPreviewUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Invoke
     *
     * @param chatLink
     */
    suspend operator fun invoke(chatLink: String) = chatRepository.openChatPreview(chatLink)
}