package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.photos.MonitorCloudDriveNodesUseCase
import javax.inject.Inject

internal class ContactFileListImageNodeFetcher @Inject constructor(
    private val monitorCloudDriveNodesUseCase: MonitorCloudDriveNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorCloudDriveNodesUseCase(
            parentId = NodeId(bundle.getLong(PARENT_ID)),
        ).mapLatest { imageNodes ->
            imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
        }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val PARENT_ID = "parentId"
    }
}
