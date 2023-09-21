package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.ResultCount
import javax.inject.Inject

/**
 * Disable export nodes use case
 *
 */
class DisableExportNodesUseCase @Inject constructor(
    private val disableExportUseCase: DisableExportUseCase
) {
    /**
     * Invoke
     *
     * @param nodeIds List of node ids
     */
    suspend operator fun invoke(nodeIds: List<NodeId>): ResultCount {
        val result = coroutineScope {
            nodeIds.map {
                async {
                    runCatching {
                        disableExportUseCase(it)
                    }
                }
            }.awaitAll()
        }
        return ResultCount(
            successCount = result.count { it.isSuccess },
            errorCount = result.count { it.isFailure }
        )
    }
}