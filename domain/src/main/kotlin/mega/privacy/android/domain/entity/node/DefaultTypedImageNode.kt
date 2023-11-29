package mega.privacy.android.domain.entity.node

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageProgress

/**
 * Default implementation of Typed Image Node
 */
internal class DefaultTypedImageNode(
    private val imageNode: ImageNode,

    /**
     *  Thumbnail path
     */
    override val thumbnailPath: String?,

    /**
     *  Preview path
     */
    override val previewPath: String?,

    /**
     *  Full Image path
     */
    override val fullSizePath: String?,

    /**
     *  Fetch Thumbnail
     */
    override val fetchThumbnail: suspend () -> String,

    /**
     *  Fetch Preview
     */
    override val fetchPreview: suspend () -> String,

    /**
     *  Fetch Full Image
     */
    override val fetchFullImage: (Boolean, () -> Unit) -> Flow<ImageProgress>,
) : TypedImageNode, ImageNode by imageNode