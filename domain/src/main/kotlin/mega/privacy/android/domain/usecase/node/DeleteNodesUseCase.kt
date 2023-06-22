package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandleUseCase
import javax.inject.Inject

/**
 * Delete nodes use case
 *
 * @property deleteNodeByHandleUseCase
 */
class DeleteNodesUseCase @Inject constructor(
    private val deleteNodeByHandleUseCase: DeleteNodeByHandleUseCase,
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param nodes
     */
    suspend operator fun invoke(nodes: List<NodeId>): MoveRequestResult.DeleteMovement {
        val results = supervisorScope {
            nodes.map { node ->
                async { runCatching { deleteNodeByHandleUseCase(node) } }
            }
        }.awaitAll()
        if (results.any { it.isSuccess }) {
            // it helps we can calculate the number of user storage
            accountRepository.resetAccountDetailsTimeStamp()
        }
        return MoveRequestResult.DeleteMovement(
            nodes.size,
            results.count { it.isFailure },
            nodes.map { it.longValue }
        )
    }
}