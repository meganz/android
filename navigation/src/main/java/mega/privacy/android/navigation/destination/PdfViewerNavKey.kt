package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

/**
 * Navigation key for the modern Compose PDF Viewer screen.
 *
 * @param nodeHandle The handle of the node to display
 * @param contentUri The content URI string for the PDF (local file path or remote URL)
 * @param isLocalContent True if content is local, false if remote streaming
 * @param shouldStopHttpServer True if HTTP server should be stopped when done (for remote content)
 * @param nodeSourceType The source type of the node (Cloud Drive, Chat, etc.)
 * @param mimeType The MIME type of the file
 * @param chatId The chat ID if opening from chat (optional)
 * @param messageId The message ID if opening from chat (optional)
 * @param isFolderLink True if opening from a folder link
 * @param title Optional title to display in the toolbar
 */
@Serializable
data class PdfViewerNavKey(
    val nodeHandle: Long,
    val contentUri: String,
    val isLocalContent: Boolean = false,
    val shouldStopHttpServer: Boolean = false,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val mimeType: String = "application/pdf",
    val chatId: Long? = null,
    val messageId: Long? = null,
    val isFolderLink: Boolean = false,
    val title: String? = null,
) : NoSessionNavKey.Optional {

    companion object {
        /**
         * Result key for node options bottom sheet results
         */
        const val RESULT = "PdfViewerNavKey:node_options_result"
    }
}
