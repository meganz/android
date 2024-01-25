package mega.privacy.android.domain.entity.node

import kotlin.time.Duration

/**
 * The typed video node entity
 *
 * @property fileNode include the all properties of default node
 * @property duration the video duration
 */
data class TypedVideoNode(
    private val fileNode: FileNode,
    val duration: Duration,
) : TypedFileNode, FileNode by fileNode
