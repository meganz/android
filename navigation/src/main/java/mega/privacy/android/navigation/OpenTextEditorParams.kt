package mega.privacy.android.navigation

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.texteditor.TextEditorMode

/**
 * Parameters for opening the text editor. Exactly one variant should be used;
 * the navigator routes to the appropriate destination (SingleActivity NavKey or legacy Intent).
 */
sealed class OpenTextEditorParams {

    /**
     * Open text editor for a cloud/explorer node (Cloud Drive, Rubbish, Folder link, etc.).
     *
     * @param fromHome True when the file is created from the Home page; affects the error message
     *   shown to the user if the upload fails in Create mode.
     */
    data class CloudNode(
        val nodeId: NodeId,
        val nodeSourceType: Int?,
        val mode: TextEditorMode,
        val fileName: String? = null,
        val fromHome: Boolean = false,
    ) : OpenTextEditorParams()

    /**
     * Open text editor for a local file (offline or zip entry).
     *
     * @param nodeSourceType [mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.OFFLINE_ADAPTER]
     *   or the zip adapter constant (2008).
     */
    data class LocalFile(
        val localPath: String,
        val fileName: String,
        val nodeSourceType: Int,
    ) : OpenTextEditorParams()

    /**
     * Open text editor for a chat message attachment.
     */
    data class Chat(
        val chatId: Long,
        val messageId: Long,
    ) : OpenTextEditorParams()

    /**
     * Open text editor for a file link. Uses legacy Activity (no SingleActivity NavKey).
     */
    data class FileLink(
        val serializedNode: String?,
        val urlFileLink: String?,
        val mode: TextEditorMode = TextEditorMode.View,
    ) : OpenTextEditorParams()
}
