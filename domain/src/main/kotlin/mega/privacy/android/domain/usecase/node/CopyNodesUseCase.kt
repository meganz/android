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
import javax.inject.Inject

/**
 * Restore nodes use case
 *
 */
class CopyNodesUseCase @Inject constructor(
    private val copyNodeUseCase: CopyNodeUseCase,
    private val accountRepository: AccountRepository,
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
                        copyNodeUseCase(NodeId(nodeHandle), NodeId(destinationHandle), null)
                    }.recover {
                        if (it.shouldEmitError()) throw it
                        return@async Result.failure(it)
                    }
                }
            }
        }.awaitAll()
        val successCount = results.count { it.isSuccess }
        if (successCount > 0) {
            accountRepository.resetAccountDetailsTimeStamp()
        }
        return MoveRequestResult.Copy(
            count = results.size,
            errorCount = results.size - successCount,
        )
    }

    private fun Throwable.shouldEmitError(): Boolean =
        this is QuotaExceededMegaException || this is NotEnoughQuotaMegaException || this is ForeignNodeException
}