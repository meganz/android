package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import javax.inject.Inject

class TimelineImageNodeFetcher @Inject constructor() : ImageNodeFetcher {

    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        TODO("Not yet implemented")
    }
}