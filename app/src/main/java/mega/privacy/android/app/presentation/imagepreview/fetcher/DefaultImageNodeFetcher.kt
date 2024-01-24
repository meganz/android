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
import mega.privacy.android.domain.usecase.MonitorImageNodesUseCase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultImageNodeFetcher @Inject constructor(
    private val monitorImageNodesUseCase: MonitorImageNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorImageNodesUseCase(
            nodeIds = bundle.getLongArray(NODE_IDS)?.map { NodeId(it) }.orEmpty(),
        ).mapLatest { imageNodes ->
            imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
        }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val NODE_IDS: String = "nodeIds"
    }
}
