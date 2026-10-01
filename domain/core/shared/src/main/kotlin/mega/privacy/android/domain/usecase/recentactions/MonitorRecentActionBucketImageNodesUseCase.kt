package mega.privacy.android.domain.usecase.recentactions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.MonitorImageNodesUseCase
import javax.inject.Inject

/**
 * Monitor the image nodes contained in a recent action bucket
 *
 * Sensitive nodes are not excluded here; the caller decides whether hidden nodes are shown.
 */
class MonitorRecentActionBucketImageNodesUseCase @Inject constructor(
    private val getRecentActionBucketByIdUseCase: GetRecentActionBucketByIdUseCase,
    private val monitorImageNodesUseCase: MonitorImageNodesUseCase,
) {

    /**
     * Monitor the image nodes contained in a recent action bucket
     *
     * Buckets are recomputed as new actions arrive, so the bucket may no longer exist by the time
     * it is opened. In that case only the anchor node is monitored so the tapped image still shows.
     *
     * @param bucketId The unique identifier of the bucket
     * @param anchorNodeId The node the user opened the bucket from
     * @return a flow of the bucket's image nodes in bucket order
     */
    operator fun invoke(bucketId: String, anchorNodeId: NodeId): Flow<List<ImageNode>> = flow {
        val nodeIds = getRecentActionBucketByIdUseCase(
            id = bucketId,
            excludeSensitives = false,
        )?.nodes?.map { it.id } ?: listOf(anchorNodeId)
        emitAll(monitorImageNodesUseCase(nodeIds))
    }
}
