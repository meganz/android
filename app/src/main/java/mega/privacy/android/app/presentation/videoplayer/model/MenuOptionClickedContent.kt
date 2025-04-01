package mega.privacy.android.app.presentation.videoplayer.model

import android.net.Uri
import nz.mega.sdk.MegaNode

/**
 * Menu option clicked content
 */
sealed interface MenuOptionClickedContent {

    /**
     * Share file content
     *
     * @property contentUri the content uri
     * @property fileName the file name
     */
    data class ShareFile(val contentUri: Uri, val fileName: String) : MenuOptionClickedContent

    /**
     * Share link content
     *
     * @property fileLink the file link
     * @property title the title
     */
    data class ShareLink(val fileLink: String?, val title: String?) : MenuOptionClickedContent

    /**
     * Share node content
     *
     * @property node the [MegaNode]
     */
    data class ShareNode(val node: MegaNode?) : MenuOptionClickedContent

    /**
     * Get link content
     *
     * @property node the [MegaNode]
     */
    data class GetLink(val node: MegaNode?) : MenuOptionClickedContent

    /**
     * Remove link content
     *
     * @property node the [MegaNode]
     */
    data class RemoveLink(val node: MegaNode?) : MenuOptionClickedContent

    /**
     * Rename content
     *
     * @property node the [MegaNode]
     */
    data class Rename(val node: MegaNode?) : MenuOptionClickedContent
}