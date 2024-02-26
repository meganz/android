package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Get cached original path for a [ChatFile] if it's cached or null otherwise
 */
class GetCachedOriginalPathUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {

    /**
     * Invoke
     *
     * @param chatFile
     *
     * @return the cached original path of this [ChatFile] or null if not cached
     */
    operator fun invoke(chatFile: ChatFile) =
        chatMessageRepository.getCachedOriginalPathForNode(chatFile.id)
}