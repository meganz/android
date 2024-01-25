package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.photos.MonitorRubbishBinNodesUseCase
import javax.inject.Inject

internal class RubbishBinImageNodeFetcher @Inject constructor(
    private val monitorRubbishBinNodesUseCase: MonitorRubbishBinNodesUseCase,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorRubbishBinNodesUseCase(
            parentId = NodeId(bundle.getLong(PARENT_ID)),
        )
    }

    internal companion object {
        const val PARENT_ID = "parentId"
    }
}
