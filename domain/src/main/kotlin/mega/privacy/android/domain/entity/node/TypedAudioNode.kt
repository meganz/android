package mega.privacy.android.domain.entity.node

/**
 * The audio node entity
 *
 * @property fileNode include the all properties of default node
 * @property duration the audio duration
 */
data class TypedAudioNode(
    private val fileNode: FileNode,
    val duration: Int,
) : TypedFileNode, FileNode by fileNode
