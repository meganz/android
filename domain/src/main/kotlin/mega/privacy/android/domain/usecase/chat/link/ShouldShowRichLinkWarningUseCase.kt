package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Should Show Rich Link Warning Use Case
 *
 */
class ShouldShowRichLinkWarningUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = repository.shouldShowRichLinkWarning()
}