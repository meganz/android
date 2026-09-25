package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * set Rich Link Warning Counter Use Case
 */
class SetRichLinkWarningCounterUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(counter: Int) = repository.setRichLinkWarningCounterValue(counter)
}