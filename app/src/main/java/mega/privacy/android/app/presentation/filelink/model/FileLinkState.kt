package mega.privacy.android.app.presentation.filelink.model

import android.content.Intent
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.folderlink.model.LinkErrorState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity]
 *
 * @property showLoginScreenEvent   Event to show login screen
 * @property url                    Url of the file
 * @property hasDbCredentials       Whether has db credentials
 * @property fileNode               Current file node
 * @property title                  Title of the current node
 * @property sizeInBytes            Size of the current file node
 * @property handle                 Handle of the current file node
 * @property previewPath            Path of the preview image
 * @property serializedData         Serialized data of the file node
 * @property iconResource           the icon resource that represents this node
 * @property askForDecryptionKeyDialogEvent Event to show AskForDecryptionDialog
 * @property collisionsEvent        Event to check with existing names
 * @property copySuccessEvent       Event to show copy was success or not
 * @property jobInProgressState     indicates if there are any job in progress that needs to be notified
 * @property openFile               State to handle file opening
 * @property downloadEvent          Event to download file with DownloadsWorker
 * @property errorMessage           State to show error message
 * @property overQuotaError         State to show over quota error
 * @property foreignNodeError       State to show foreign node error
 * @property shouldShowAdsForLink   Whether should show ads
 * @property errorState             Error state of the file link
 */
data class FileLinkState(
    val showLoginScreenEvent: StateEvent = consumed,
    val hasDbCredentials: Boolean = false,
    val url: String = "",
    val fileNode: TypedFileNode? = null,
    val title: String = "",
    val sizeInBytes: Long = 0,
    val handle: Long = -1,
    val previewPath: String? = null,
    val serializedData: String? = null,
    val iconResource: Int? = null,
    val jobInProgressState: FileLinkJobInProgressState? = FileLinkJobInProgressState.InitialLoading,
    val askForDecryptionKeyDialogEvent: StateEvent = consumed,
    val collisionsEvent: StateEventWithContent<NameCollision> = consumed(),
    val copySuccessEvent: StateEvent = consumed,
    val openFile: StateEventWithContent<Intent> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent.DownloadTriggerEvent> = consumed(),
    val errorMessage: StateEventWithContent<Int> = consumed(),
    val overQuotaError: StateEventWithContent<StorageState> = consumed(),
    val foreignNodeError: StateEvent = consumed,
    val shouldShowAdsForLink: Boolean = false,
    val errorState: LinkErrorState = LinkErrorState.NoError
) {
    /**
     * Whether to show toolbar and bottom bar actions
     */
    val showContentActions: Boolean
        get() = errorState == LinkErrorState.NoError && jobInProgressState != FileLinkJobInProgressState.InitialLoading

    /**
     * Creates a copy of this view state with the info that can be extracted directly from typedNode
     */
    fun copyWithTypedNode(typedNode: TypedFileNode, iconResource: Int) = this.copy(
        fileNode = typedNode,
        title = typedNode.name,
        sizeInBytes = typedNode.size,
        previewPath = typedNode.previewPath,
        iconResource = if (typedNode.previewPath == null) iconResource else null,
        handle = typedNode.id.longValue,
        serializedData = typedNode.serializedData
    )
}
