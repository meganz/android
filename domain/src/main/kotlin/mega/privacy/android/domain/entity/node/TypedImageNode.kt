package mega.privacy.android.domain.entity.node

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageProgress

/**
 * Typed Image Node
 */
class TypedImageNode(
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
    val fetchThumbnail: suspend () -> String,

    /**
     *  Fetch Preview
     */
    val fetchPreview: suspend () -> String,

    /**
     *  Fetch Full Image
     */
    val fetchFullImage: (Boolean, () -> Unit) -> Flow<ImageProgress>,
) : TypedFileNode, FileNode by imageNode