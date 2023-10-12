package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.usecase.photos.MonitorTimelineNodesUseCase
import javax.inject.Inject

class TimelineImageNodeFetcher @Inject constructor(
    private val monitorTimelineNodesUseCase: MonitorTimelineNodesUseCase,
) : ImageNodeFetcher {

    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorTimelineNodesUseCase()
    }
}
