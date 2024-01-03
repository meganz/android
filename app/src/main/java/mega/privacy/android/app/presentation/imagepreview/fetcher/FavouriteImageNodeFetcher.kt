package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.MonitorFavouriteImageNodesUseCase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
internal class FavouriteImageNodeFetcher @Inject constructor(
    private val monitorFavouriteImageNodesUseCase: MonitorFavouriteImageNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorFavouriteImageNodesUseCase(
            nodeId = NodeId(bundle.getLong(NODE_ID)),
            count = 1,
        ).mapLatest { imageNodes ->
            imageNodes.sortedByDescending { imageNode -> imageNode.modificationTime }
        }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val NODE_ID: String = "nodeId"
    }
}
