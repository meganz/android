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
        val nodeIds = bundle.getLongArray(NODE_IDS)?.distinct()?.map { NodeId(it) }.orEmpty()
        val isOrderPreserved = bundle.getBoolean(PRESERVE_ORDER, false)
        val positions = nodeIds.withIndex().associate { (index, nodeId) -> nodeId to index }
        return monitorImageNodesUseCase(
            nodeIds = nodeIds,
        ).mapLatest { imageNodes ->
            if (isOrderPreserved) {
                imageNodes.sortedBy { positions[it.id] ?: Int.MAX_VALUE }
            } else {
                imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
            }
        }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val NODE_IDS: String = "nodeIds"

        /**
         * When true, the images are shown in the order of [NODE_IDS] instead of by
         * modification time, so that the viewer pages in the same order as the caller's list.
         */
        const val PRESERVE_ORDER: String = "preserveOrder"
    }
}
