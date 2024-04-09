package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.RegexRepository
import javax.inject.Inject

/**
 * Get links from message content use case.
 */
class GetLinksFromMessageContentUseCase @Inject constructor(
    private val regexRepository: RegexRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke(messageContent: String) = buildList {
        with(regexRepository.webUrlPattern.matcher(messageContent)) {
            while (find()) {
                add(group())
            }
        }
    }
}