package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.photos.MonitorFolderLinkMediaDiscoveryNodesUseCase
import javax.inject.Inject

internal class FolderLinkMediaDiscoveryImageNodeFetcher @Inject constructor(
    private val monitorFolderLinkMediaDiscoveryNodesUseCase: MonitorFolderLinkMediaDiscoveryNodesUseCase,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorFolderLinkMediaDiscoveryNodesUseCase(
            parentId = NodeId(bundle.getLong(PARENT_ID)),
            recursive = true,
        )
    }

    internal companion object {
        const val PARENT_ID = "parentId"
    }
}