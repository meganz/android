package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.chat.GetChatFilesUseCase
import javax.inject.Inject

/**
 * Copies the nodes of resolved name collisions to their destination, reaching each node the way
 * its collision allows: through the account tree, a public link or a chat attachment.
 */
class CopyCollidedNodesUseCase @Inject constructor(
    private val getChatFilesUseCase: GetChatFilesUseCase,
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase,
    private val nodeRepository: NodeRepository,
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param nameCollisions The collisions whose nodes to copy.
     * @param rename True to copy each node under its collision's rename name, false to keep the name.
     * @return How many nodes were and were not copied. A quota or foreign-node failure is thrown
     * instead of being counted, because it stops the whole batch.
     */
    suspend operator fun invoke(
        nameCollisions: List<NodeNameCollision>,
        rename: Boolean,
    ): MoveRequestResult.Copy {
        val results = coroutineScope {
            val semaphore = Semaphore(10)
            nameCollisions.map { nameCollision ->
                async {
                    semaphore.withPermit {
                        runCatching { copy(nameCollision, rename) }
                            .onFailure { if (it.shouldEmitErrorForNodeMovement()) throw it }
                    }
                }
            }.awaitAll()
        }
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            accountRepository.resetAccountDetailsTimeStamp()
        }
        return MoveRequestResult.Copy(
            count = results.size,
            errorCount = results.size - successCount,
        )
    }

    private suspend fun copy(nameCollision: NodeNameCollision, rename: Boolean) {
        val newNodeParent = NodeId(nameCollision.parentHandle)
        val newNodeName = if (rename) nameCollision.renameName else null
        when (nameCollision) {
            is NodeNameCollision.Chat -> {
                val chatNode = getChatFilesUseCase(nameCollision.chatId, nameCollision.messageId)
                    .firstOrNull { it.id.longValue == nameCollision.nodeHandle }
                requireNotNull(chatNode) { "Chat file not found" }
                copyTypedNodeUseCase(
                    nodeToCopy = chatNode,
                    newNodeParent = newNodeParent,
                    newNodeName = newNodeName,
                )
            }

            is NodeNameCollision.FileLink -> {
                val publicNode = nodeRepository.getNodeFromSerializedData(nameCollision.serializedData)
                requireNotNull(publicNode) { "Public node could not be deserialized" }
                nodeRepository.copyPublicNode(
                    publicNodeToCopy = publicNode,
                    newNodeParent = newNodeParent,
                    newNodeName = newNodeName,
                )
            }

            is NodeNameCollision.Default -> nodeRepository.copyNode(
                nodeToCopy = NodeId(nameCollision.nodeHandle),
                newNodeParent = newNodeParent,
                newNodeName = newNodeName,
            )
        }
    }
}
