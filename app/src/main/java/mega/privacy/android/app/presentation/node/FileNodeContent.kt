package mega.privacy.android.app.presentation.node

import mega.privacy.android.domain.entity.node.NodeContentUri
import java.io.File

/**
 * File node content
 *
 */
sealed interface FileNodeContent {
    /**
     * Image for chat
     *
     * @property allAttachmentMessageIds
     */
    data class ImageForChat(val allAttachmentMessageIds: List<Long>) : FileNodeContent

    /**
     * Image for node
     * @param isImagePreview
     */
    data class ImageForNode(val isImagePreview: Boolean) : FileNodeContent
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
     * Url content
     * @property uri [NodeContentUri]
     * @property path [String]
     */
    data class UrlContent(val uri: NodeContentUri, val path: String?) : FileNodeContent

    /**
     * Other content
     *
     * @property localFile
     */
    data class Other(val localFile: File?) : FileNodeContent
}