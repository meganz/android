package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.ensureActive
import mega.privacy.android.domain.entity.node.MultipleNodesRestoreResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.RestoreNodeResult
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.exception.node.NodeInRubbishException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Restore nodes use case
 *
 */
class RestoreNodesUseCase @Inject constructor(
    private val moveNodeUseCase: MoveNodeUseCase,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param nodes
     * @return
     */
    suspend operator fun invoke(nodes: Map<Long, Long>): RestoreNodeResult {
        val results = nodes.map { entry ->
            val (nodeHandle, destinationHandle) = entry
            runCatching {
                if (isNodeInRubbish(destinationHandle)) throw NodeInRubbishException()
                moveNodeUseCase(NodeId(nodeHandle), NodeId(destinationHandle))
            }.also {
                coroutineContext.ensureActive()
            }
        }
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            accountRepository.resetAccountDetailsTimeStamp()
        }
        return when {
            (results.any { it.exceptionOrNull() is ForeignNodeException }) -> throw ForeignNodeException()
            results.size == 1 -> SingleNodeRestoreResult(
                successCount = successCount,
                destinationFolderName = takeIf { successCount > 0 }
                    ?.let { getNodeByHandleUseCase(nodes.values.first())?.name }
            )

            else -> MultipleNodesRestoreResult(
                successCount = successCount,
                errorCount = results.size - successCount,
            )
        }
    }
}