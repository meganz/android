package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.usecase.MonitorFavouriteImageNodesUseCase
import javax.inject.Inject

internal class FavouriteImageNodeFetcher @Inject constructor(
    private val monitorFavouriteImageNodesUseCase: MonitorFavouriteImageNodesUseCase,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> =
        monitorFavouriteImageNodesUseCase()
}
