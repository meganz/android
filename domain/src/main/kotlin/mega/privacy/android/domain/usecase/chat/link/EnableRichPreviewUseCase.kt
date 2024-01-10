package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Enable Rich Preview Use Case
 *
 */
class EnableRichPreviewUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(enabled: Boolean) = repository.enableRichPreviews(enabled)
}