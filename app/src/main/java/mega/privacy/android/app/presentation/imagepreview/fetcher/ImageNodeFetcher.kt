package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode

interface ImageNodeFetcher {
    /**
     * @param bundle is the optional primitive parameters support that might be needed for nodes fetching, e.g., parentHandle, folderLink, chatRoomId, etc.
     */
    fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>>
}
