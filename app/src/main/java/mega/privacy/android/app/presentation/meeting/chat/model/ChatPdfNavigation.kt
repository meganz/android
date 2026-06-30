package mega.privacy.android.app.presentation.meeting.chat.model

import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.navigation.destination.PdfViewerNavKey

/**
 * Describes how a PDF attachment opened from chat should be displayed. The flag decision is made in
 * the ViewModel; the view only routes the result, keeping the [PdfViewerNavKey] navigation in-place
 * via the host's NavigationHandler instead of leaking the handler into business code.
 */
sealed interface ChatPdfNavigation {

    /**
     * Open the Compose PDF viewer in-place within the chat host's back stack.
     *
     * @property navKey destination consumed by the host's NavigationHandler.
     */
    data class InPlace(val navKey: PdfViewerNavKey) : ChatPdfNavigation

    /**
     * Open the legacy `PdfViewerActivity` (feature flag disabled).
     */
    data class Legacy(
        val content: NodeContentUri,
        val nodeHandle: Long,
        val chatId: Long,
        val messageId: Long,
        val mimeType: String,
    ) : ChatPdfNavigation
}
