package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.photos.MonitorCloudDriveNodesUseCase
import javax.inject.Inject

class SharedItemsImageNodeFetcher @Inject constructor(
    private val monitorCloudDriveNodesUseCase: MonitorCloudDriveNodesUseCase,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorCloudDriveNodesUseCase(
            parentId = NodeId(bundle.getLong(PARENT_ID)),
        )
    }

    internal companion object {
        const val PARENT_ID = "parentId"
    }
}