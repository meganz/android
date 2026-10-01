package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Is Rich Previews Enabled Use Case
 *
 */
class IsRichPreviewsEnabledUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = repository.isRichPreviewsEnabled()
}