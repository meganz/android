package mega.privacy.android.domain.entity.node

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageProgress

/**
 * Typed Image Node
 */
interface TypedImageNode : TypedFileNode, ImageNode {
    /**
     *  Fetch Thumbnail
     */
    val fetchThumbnail: suspend () -> String

    /**
     *  Fetch Preview
     */
    val fetchPreview: suspend () -> String

    /**
     *  Fetch Full Image
     */
    val fetchFullImage: (Boolean, () -> Unit) -> Flow<ImageProgress>
}