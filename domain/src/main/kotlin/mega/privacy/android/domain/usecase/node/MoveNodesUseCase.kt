package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Restore nodes use case
 *
 */
class MoveNodesUseCase @Inject constructor(
    private val moveNodeUseCase: MoveNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val accountRepository: AccountRepository,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodes key node to move, value target node
     * @return
     */
    suspend operator fun invoke(nodes: Map<Long, Long>): MoveRequestResult {
        val results = coroutineScope {
            nodes.map { (nodeHandle, destinationHandle) ->
                async {
                    runCatching {
                        moveNodeUseCase(NodeId(nodeHandle), NodeId(destinationHandle))
                    }.recover {
                        if (it.shouldEmitError()) throw it
                        return@async Result.failure(it)
                    }
                }
            }
        }.awaitAll()
        val oldParentHandle = getOldParentHandle(nodes.size == 1, nodes.keys.first())
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            accountRepository.resetAccountDetailsTimeStamp()
        }
        return MoveRequestResult.GeneralMovement(
            count = results.size,
            errorCount = results.size - successCount,
            oldParentHandle = oldParentHandle,
            nodes = nodes.keys.toList()
        )
    }

    private suspend fun getOldParentHandle(isSingle: Boolean, handle: Long) = if (isSingle) {
        getNodeByHandleUseCase(handle, true)?.parentId?.longValue
    } else {
        nodeRepository.getInvalidHandle()
    }

    private fun Throwable.shouldEmitError(): Boolean =
        this is QuotaExceededMegaException || this is NotEnoughQuotaMegaException || this is ForeignNodeException
}