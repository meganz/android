package mega.privacy.android.domain.entity.node

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
     */
    data object ImageForNode : FileNodeContent

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
    open class Other(open val localFile: File?) : FileNodeContent
    /**
     * Local Zip file
     */
    class LocalZipFile(override val localFile: File) : Other(localFile)
}