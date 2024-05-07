package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 *  Use Case to copy list of collided nodes
 */
class CopyCollidedNodesUseCase @Inject constructor(
    private val copyCollidedNodeUseCase: CopyCollidedNodeUseCase,
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param nameCollisions The list of NodeNameCollision
     * @param rename True if the node should be renamed, false otherwise
     * @return MoveRequestResult
     */
    suspend operator fun invoke(
        nameCollisions: List<NodeNameCollision>,
        rename: Boolean,
    ): MoveRequestResult {
        val results = coroutineScope {
            val semaphore = Semaphore(10)
            nameCollisions.map { nameCollision ->
                async {
                    semaphore.withPermit {
                        runCatching {
                            copyCollidedNodeUseCase(
                                nameCollision = nameCollision,
                                rename = rename,
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
        ).also {
            if (successCount > 0) {
                accountRepository.resetAccountDetailsTimeStamp()
            }
        }
    }
}