package mega.privacy.android.domain.entity.node

/**
 * The video node entity
 *
 * @property fileNode include the all properties of default node
 * @property duration the video duration
 * @property thumbnailFilePath the thumbnail file path for loading thumbnail
 */
data class TypedVideoNode(
    private val fileNode: FileNode,
    val duration: Int,
    val thumbnailFilePath: String?,
) : TypedFileNode, FileNode by fileNode
