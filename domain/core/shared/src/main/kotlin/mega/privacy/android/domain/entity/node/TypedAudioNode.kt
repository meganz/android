package mega.privacy.android.domain.entity.node

import kotlin.time.Duration

/**
 * The audio node entity
 *
 * @property fileNode include the all properties of default node
 * @property duration the audio duration
 */
data class TypedAudioNode(
    private val fileNode: FileNode,
    val duration: Duration,
) : TypedFileNode, FileNode by fileNode
