package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
import javax.inject.Inject

/**
 *  Use Case to copy list of chat nodes
 */
class CopyChatNodesUseCase @Inject constructor(
    private val copyChatNodeUseCase: CopyChatNodeUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId The ID of the chat
     * @param messageIds The list of message IDs
     * @param newNodeParent the Node when the chat node will be moved to
     * @return MoveRequestResult
     */
    suspend operator fun invoke(
        chatId: Long,
        messageIds: List<Long>,
        newNodeParent: NodeId,
    ): MoveRequestResult {
        val results = coroutineScope {
            val semaphore = Semaphore(10)
            messageIds.map { messageId ->
                async {
                    semaphore.withPermit {
                        runCatching {
                            copyChatNodeUseCase(
                                chatId = chatId,
                                messageId = messageId,
                                messageIndex = 0,
                                newNodeName = null,
                                newNodeParent = newNodeParent
                            )
                        }.recover {
                            if (it.shouldEmitErrorForNodeMovement()) throw it
                            return@async Result.failure(it)
                        }
                    }
                }
            }
        }.awaitAll()
        val successCount = results.count { it.isSuccess }
        return MoveRequestResult.Copy(
            count = results.size,
            errorCount = results.size - successCount,
        )
    }
}