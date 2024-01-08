package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Monitor Rich Link Preview Config
 *
 */
class MonitorRichLinkPreviewConfigUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke() = repository.monitorRichLinkPreviewConfig()
}