package mega.privacy.android.feature.pdfviewer.presentation.model

import android.net.Uri
import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * Represents the source of a PDF document.
 *
 * This sealed class abstracts the different ways a PDF can be loaded,
 * replacing the legacy adapter type pattern.
 */
internal sealed class PdfViewerSource {
    /**
     * Whether this source requires loading bytes from a remote URL.
     * Remote sources need to fetch bytes before the PDF can be displayed.
     */
    abstract val isRemote: Boolean

    /**
     * PDF from a MEGA cloud node (Cloud Drive, Rubbish Bin, or Backups).
     *
     * @param nodeHandle The node handle
     * @param contentUri The content URI string (local path or remote URL)
     * @param isLocalContent True if content is local
     * @param nodeSourceType The specific source type (CLOUD_DRIVE, RUBBISH_BIN, or BACKUPS)
     */
    data class CloudNode(
        val nodeHandle: Long,
        val contentUri: String,
        val isLocalContent: Boolean,
        val nodeSourceType: NodeSourceType,
    ) : PdfViewerSource() {
        override val isRemote: Boolean get() = !isLocalContent
    }

    /**
     * PDF saved for offline
     */
    data class Offline(
        val handle: String,
        val localPath: String,
    ) : PdfViewerSource() {
        override val isRemote: Boolean get() = false
    }

    /**
     * PDF from chat attachment
     *
     * @param chatId The chat ID
     * @param messageId The message ID
     * @param nodeHandle The node handle
     * @param contentUri The content URI string (local path or remote URL)
     * @param isLocalContent True if content is local
     */
    data class ChatAttachment(
        val chatId: Long,
        val messageId: Long,
        val nodeHandle: Long,
        val contentUri: String,
        val isLocalContent: Boolean,
    ) : PdfViewerSource() {
        override val isRemote: Boolean get() = !isLocalContent
    }

    /**
     * PDF from file link
     *
     * @param serializedNode The serialized node string
     * @param url The file link URL
     * @param contentUri The content URI string (local path or remote URL)
     * @param isLocalContent True if content is local
     */
    data class FileLink(
        val serializedNode: String,
        val url: String?,
        val contentUri: String,
        val isLocalContent: Boolean,
    ) : PdfViewerSource() {
        override val isRemote: Boolean get() = !isLocalContent
    }

    /**
     * PDF from folder link
     *
     * @param nodeHandle The node handle
     * @param contentUri The content URI string (local path or remote URL)
     * @param isLocalContent True if content is local
     */
    data class FolderLink(
        val nodeHandle: Long,
        val contentUri: String,
        val isLocalContent: Boolean,
    ) : PdfViewerSource() {
        override val isRemote: Boolean get() = !isLocalContent
    }

    /**
     * PDF from ZIP file
     */
    data class ZipFile(
        val uri: Uri,
    ) : PdfViewerSource() {
        override val isRemote: Boolean get() = false
    }

    /**
     * PDF from external intent (file open)
     */
    data class ExternalFile(
        val uri: Uri,
        val fileName: String?,
    ) : PdfViewerSource() {
        override val isRemote: Boolean get() = false
    }
}
