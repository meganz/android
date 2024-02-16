package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.usecase.photos.MonitorZipImageNodesUseCase
import javax.inject.Inject

internal class ZipImageNodeFetcher @Inject constructor(
    private val monitorZipImageNodesUseCase: MonitorZipImageNodesUseCase,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorZipImageNodesUseCase(
            uriString = bundle.getString(URI).orEmpty(),
        )
    }

    internal companion object {
        const val URI = "uri"
    }
}
