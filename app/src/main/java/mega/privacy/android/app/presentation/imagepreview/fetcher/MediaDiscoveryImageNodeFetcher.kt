package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.photos.MonitorMediaDiscoveryNodesUseCase
import javax.inject.Inject

class MediaDiscoveryImageNodeFetcher @Inject constructor(
    private val monitorMediaDiscoveryNodesUseCase: MonitorMediaDiscoveryNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorMediaDiscoveryNodesUseCase(
            parentId = NodeId(bundle.getLong(PARENT_ID)),
            recursive = bundle.getBoolean(IS_RECURSIVE),
        ).mapLatest { imageNodes ->
            when (bundle.serializable<Sort>(IMAGE_PREVIEW_SORT)) {
                Sort.NEWEST -> imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
                Sort.OLDEST -> imageNodes.sortedWith(compareBy<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
                else -> imageNodes
            }
        }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val PARENT_ID = "parentId"

        const val IS_RECURSIVE = "recursive"
        const val IMAGE_PREVIEW_SORT = "image_preview_sort"
    }
}