package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.folderpreference.GetFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.photos.MonitorCloudDriveNodesUseCase
import javax.inject.Inject

class CloudDriveImageNodeFetcher @Inject constructor(
    private val monitorCloudDriveNodesUseCase: MonitorCloudDriveNodesUseCase,
    private val getFolderSortOrderUseCase: GetFolderSortOrderUseCase,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> = flow {
        val parentId = NodeId(bundle.getLong(PARENT_ID))
        emitAll(
            monitorCloudDriveNodesUseCase(
                parentId = parentId,
                sortOrder = getFolderSortOrderUseCase(parentId),
            )
        )
    }

    internal companion object {
        const val PARENT_ID = "parentId"
    }
}