package mega.privacy.android.app.presentation.transfers.model.completed

import android.net.Uri
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import java.io.File

/**
 * UI state for completed transfer actions.
 *
 * @property completedTransfer The completed transfer.
 * @property canViewInFolder Indicates if the user can view the completed transfer in its folder.
 * @property canOpenWith Indicates if the user can open the completed transfer with another app.
 * @property canShareLink Indicates if the user can share a link to the completed transfer.
 * @property parentUri The URI of the parent folder of the completed transfer.
 * @property fileUri The URI of the completed transfer file.
 * @property amINodeOwner Indicates if the user is the owner of the node associated with the completed transfer.
 * @property isOnline Indicates if the device is currently online.
 * @property openWithEvent Event to handle opening the completed transfer with another app.
 * @property shareLinkEvent Event to handle sharing a link to the completed transfer.
 */
data class CompletedTransferActionsUiState(
    val completedTransfer: CompletedTransfer? = null,
    val parentUri: Uri? = null,
    val fileUri: Uri? = null,
    val amINodeOwner: Boolean = false,
    val isOnline: Boolean = true,
    val openWithEvent: StateEventWithContent<OpenWithEvent> = consumed(),
    val shareLinkEvent: StateEventWithContent<ShareLinkEvent> = consumed(),
) {
    val canViewInFolder
        get() = completedTransfer != null &&
                ((completedTransfer.isContentUriDownload && fileUri != null)
                        || completedTransfer.isContentUriDownload.not())

    val canOpenWith
        get() = completedTransfer != null
                && completedTransfer.type.isDownloadType()
                && fileUri != null

    val canShareLink
        get() = completedTransfer != null && amINodeOwner
}

/**
 * Data class representing an event to open a file with another app.
 *
 * @property file The file to be opened, if applicable.
 * @property uri The URI to be opened, if applicable.
 * @property fileType The type of the file, if applicable.
 * @property isValid Indicates if the event is valid, meaning either a file or URI is provided.
 *
 */
data class OpenWithEvent(
    val file: File? = null,
    val uri: Uri? = null,
    val fileType: String? = null,
) {
    val isValid: Boolean
        get() = file != null || uri != null
}

/**
 * Data class representing an event to share a link to a completed transfer.
 *
 * @property node The node associated with the completed transfer, if applicable.
 * @property isValid Indicates if the event is valid, meaning the node is not null.
 * @property isTakenDown Indicates if the node has been taken down.
 */
data class ShareLinkEvent(
    val node: UnTypedNode? = null,
) {
    val isValid: Boolean
        get() = node != null

    val isTakenDown: Boolean
        get() = node?.isTakenDown == true
}
