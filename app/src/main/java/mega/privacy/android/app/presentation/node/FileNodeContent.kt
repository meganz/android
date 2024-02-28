package mega.privacy.android.app.presentation.node

import mega.privacy.android.domain.entity.node.NodeContentUri
import java.io.File

/**
 * File node content
 *
 */
sealed interface FileNodeContent {
    /**
     * Image
     *
     * @property allAttachmentMessageIds
     */
    data class Image(val allAttachmentMessageIds: List<Long>) : FileNodeContent

    /**
     * Text content
     *
     */
    data object TextContent : FileNodeContent

    /**
     * Pdf content
     *
     * @property uri
     */
    data class Pdf(val uri: NodeContentUri) : FileNodeContent

    /**
     * Audio or video content
     *
     * @property uri
     */
    data class AudioOrVideo(val uri: NodeContentUri) : FileNodeContent

    /**
     * Other content
     *
     * @property localFile
     */
    data class Other(val localFile: File?) : FileNodeContent
}