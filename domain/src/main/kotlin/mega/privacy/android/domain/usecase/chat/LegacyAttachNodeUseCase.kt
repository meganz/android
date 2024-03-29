package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Attach node use case
 *
 * @property chatRepository [ChatRepository]
 */
@Deprecated("Deprecated. Replace with AttachNodeUseCase")
class LegacyAttachNodeUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat identifier.
     * @param nodeHandle Node identifier.
     */
    suspend operator fun invoke(chatId: Long, nodeId: NodeId) =
        chatRepository.attachNode(chatId, nodeId.longValue)
}