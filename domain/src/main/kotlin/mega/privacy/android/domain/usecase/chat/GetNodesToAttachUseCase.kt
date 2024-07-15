package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.StorageStatePayWallException
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.message.GetAttachableNodeIdUseCase
import javax.inject.Inject

/**
 * Get nodes to attach to a chat use case
 *
 */
class GetNodesToAttachUseCase @Inject constructor(
    private val getAttachableNodeIdUseCase: GetAttachableNodeIdUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {
    /**
     * Invoke
     *
     * @param nodeIds List of [NodeId] to get the attachable nodes
     * @return List of [NodeId] that can be used to attach the nodes to a chat
     */
    suspend operator fun invoke(nodeIds: List<NodeId>): List<NodeId> {
        if (monitorStorageStateEventUseCase().value.storageState == StorageState.PayWall) {
            throw StorageStatePayWallException()
        }
        val semaphore = Semaphore(10)
        return coroutineScope {
            nodeIds.map { id ->
                async {
                    semaphore.withPermit {
                        runCatching {
                            val node = getNodeByIdUseCase(id)
                            if (node is TypedFileNode) {
                                getAttachableNodeIdUseCase(node)
                            } else {
                                null
                            }
                        }.getOrNull()
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }
}