package mega.privacy.android.navigation.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

/**
 * Navigation key for the modern Compose PDF Viewer screen.
 *
 * @param nodeHandle The handle of the node to display. Use -1L for external files (no MEGA node).
 * @param contentUri The content URI string for the PDF (local file path or remote URL)
 * @param isLocalContent True if content is local, false if remote streaming
 * @param shouldStopHttpServer True if HTTP server should be stopped when done (for remote content)
 * @param nodeSourceType The source type of the node (cloud drive, chat, folder link, etc.)
 * @param mimeType The MIME type of the file
 * @param chatId The chat ID if opening from chat (optional)
 * @param messageId The message ID if opening from chat (optional)
 * @param title Optional title to display in the toolbar
 * @param isExternalFile True if the PDF was opened from an external app intent (no MEGA node)
 * @param publicLinkUrl The public file-link URL, when the PDF is opened from a file link. Used to
 *  resolve the public node for the toolbar actions, since a file-link node is not in the account.
 */
@Serializable
@Parcelize
data class PdfViewerNavKey(
    val nodeHandle: Long = -1L,
    val contentUri: String,
    val isLocalContent: Boolean = false,
    val shouldStopHttpServer: Boolean = false,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val mimeType: String = "application/pdf",
    val chatId: Long? = null,
    val messageId: Long? = null,
    val title: String? = null,
    val isExternalFile: Boolean = false,
    val publicLinkUrl: String? = null,
) : NoSessionNavKey.Optional, Parcelable {

    companion object {
        /**
         * Result key for node options bottom sheet results
         */
        const val RESULT = "PdfViewerNavKey:node_options_result"
    }
}
