package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import mega.privacy.android.domain.entity.node.MultipleNodesRestoreResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.RestoreNodeResult
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.GetNodeNameByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import javax.inject.Inject

/**
 * Restore nodes use case
 *
 */
class RestoreNodesUseCase @Inject constructor(
    private val moveNodeUseCase: MoveNodeUseCase,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val getNodeNameByIdUseCase: GetNodeNameByIdUseCase,
    private val accountRepository: AccountRepository,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
) {
    /**
     * Invoke
     *
     * @param nodes
     * @return
     */
    suspend operator fun invoke(nodes: Map<Long, Long>): RestoreNodeResult {
        val results = supervisorScope {
            nodes.map { entry ->
                async {
                    val (nodeHandle, destinationHandle) = entry
                    val actualDestinationHandle =
                        if (isNodeInRubbishOrDeletedUseCase(destinationHandle)) {
                            getRootNodeIdUseCase()?.longValue ?: destinationHandle
                        } else {
                            destinationHandle
                        }

                    val result = runCatching {
                        moveNodeUseCase(NodeId(nodeHandle), NodeId(actualDestinationHandle))
                    }
                    Pair(result, actualDestinationHandle)
                }
            }
        }.awaitAll()
        val successCount = results.count { it.first.isSuccess }
        if (successCount > 0) {
            accountRepository.resetAccountDetailsTimeStamp()
        }
        return when {
            (results.any { it.first.exceptionOrNull() is ForeignNodeException }) -> throw ForeignNodeException()
            results.size == 1 -> SingleNodeRestoreResult(
                successCount = successCount,
                destinationFolderName = takeIf { successCount > 0 }
                    ?.let {
                        val actualDestinationHandle = results.first().second
                        getNodeNameByIdUseCase(NodeId(actualDestinationHandle))
                    }
            )

            else -> MultipleNodesRestoreResult(
                successCount = successCount,
                errorCount = results.size - successCount,
            )
        }
    }
}
