package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.MonitorOfflineImageNodesUseCase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
internal class OfflineImageNodeFetcher @Inject constructor(
    private val monitorOfflineNodesUseCase: MonitorOfflineImageNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorOfflineNodesUseCase(path = bundle.getString(PATH, ""))
            .mapLatest { imageNodes ->
                imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
            }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val PATH: String = "path"
    }
}
