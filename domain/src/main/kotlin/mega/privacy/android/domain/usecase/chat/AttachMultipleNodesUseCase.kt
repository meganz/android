package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import mega.privacy.android.domain.entity.node.ChatRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case to attach multiple node use case to chat
 */
class AttachMultipleNodesUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository
) {
    /**
     * Invoke
     * @param nodeIds List of [NodeId]
     * @param chatIds [LongArray]
     * @return [ChatRequestResult.ChatRequestAttachNode]
     */
    suspend operator fun invoke(
        nodeIds: List<NodeId>,
        chatIds: LongArray,
    ): ChatRequestResult.ChatRequestAttachNode {
        val results = supervisorScope {
            nodeIds.map { nodeId ->
                async {
                    runCatching {
                        chatIds.map { chatId ->
                            chatMessageRepository.attachNode(chatId = chatId, nodeId = nodeId)
                        }
                    }.recover {
                        return@async Result.failure(it)
                    }
                }
            }.awaitAll()
        }
        val successCount = results.count { it.isSuccess }
        return ChatRequestResult.ChatRequestAttachNode(
            count = results.size,
            errorCount = results.size - successCount
        )
    }
}