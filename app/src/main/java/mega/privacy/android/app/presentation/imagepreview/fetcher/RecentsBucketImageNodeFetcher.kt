package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.recentactions.MonitorRecentActionBucketImageNodesUseCase
import javax.inject.Inject

internal class RecentsBucketImageNodeFetcher @Inject constructor(
    private val monitorRecentActionBucketImageNodesUseCase: MonitorRecentActionBucketImageNodesUseCase,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorRecentActionBucketImageNodesUseCase(
            bucketId = bundle.getString(BUCKET_ID).orEmpty(),
            anchorNodeId = NodeId(bundle.getLong(ANCHOR_NODE_ID)),
        )
    }

    internal companion object {
        const val BUCKET_ID: String = "bucketId"
        const val ANCHOR_NODE_ID: String = "anchorNodeId"
    }
}
