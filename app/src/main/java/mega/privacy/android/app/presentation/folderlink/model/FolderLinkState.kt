package mega.privacy.android.app.presentation.folderlink.model

import android.content.Intent
import androidx.annotation.StringRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.myAccount.StorageStatusDialogState
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.folderlink.FolderLinkViewModel]
 *
 * @property isInitialState             Whether it is initial state
 * @property url                        Url of the folder
 * @property folderSubHandle            Handle of the folder link
 * @property isLoginComplete            Whether is login is successfully completed
 * @property isNodesFetched             Whether nodes are fetched
 * @property hasDbCredentials           Whether db credentials are valid
 * @property hasMediaItem               Whether current folder has any image/video
 * @property nodesList                  List of nodes to show
 * @property rootNode                   Root node
 * @property parentNode                 Parent node of the nodes shown
 * @property currentViewType            Whether list or grid view
 * @property title                      Title of the folder
 * @property selectedNodeCount          Count of nodes selected
 * @property askForDecryptionKeyDialogEvent  Event to show AskForDecryptionDialog
 * @property collisionsEvent            Event with list of nodes with existing names
 * @property copyResultEvent            Event to show on successful copy text or error
 * @property showLoginEvent             Event to show login screen
 * @property finishActivityEvent        Event to finish the activity
 * @property openFile                   State to handle file opening
 * @property downloadEvent              Event to download nodes with DownloadsWorker
 * @property importNode                 Node to import
 * @property selectImportLocation       State to open location selection
 * @property snackbarMessageContent     State to show snackbar message
 * @property openMoreOption             State to open more option bottom sheet
 * @property moreOptionNode             Node to show more options for
 * @property storageStatusDialogState   State of StorageStatusDialog
 * @property showErrorDialogEvent       Event to show error dialog with String id of title and content
 * @property snackBarMessage            String id of content for snack bar
 * @property shouldShowAdsForLink       Whether ads should be shown for the link
 */
data class FolderLinkState(
    val isInitialState: Boolean = true,
    val url: String? = null,
    val folderSubHandle: String? = null,
    val isLoginComplete: Boolean = false,
    val isNodesFetched: Boolean = false,
    val hasDbCredentials: Boolean = false,
    val hasMediaItem: Boolean = false,
    val nodesList: List<NodeUIItem<TypedNode>> = listOf(),
    val rootNode: TypedFolderNode? = null,
    val parentNode: TypedFolderNode? = null,
    val currentViewType: ViewType = ViewType.LIST,
    val title: String = "",
    val selectedNodeCount: Int = 0,
    val importNode: NodeUIItem<TypedNode>? = null,
    @StringRes val snackBarMessage: Int = -1,
    val moreOptionNode: NodeUIItem<TypedNode>? = null,
    val storageStatusDialogState: StorageStatusDialogState? = null,
    val showLoginEvent: StateEvent = consumed,
    val finishActivityEvent: StateEvent = consumed,
    val askForDecryptionKeyDialogEvent: StateEvent = consumed,
    val selectImportLocation: StateEvent = consumed,
    val openMoreOption: StateEvent = consumed,
    val collisionsEvent: StateEventWithContent<List<NameCollision>> = consumed(),
    val copyResultEvent: StateEventWithContent<Pair<String?, Throwable?>> = consumed(),
    val showErrorDialogEvent: StateEventWithContent<Pair<Int, Int>> = consumed(),
    val openFile: StateEventWithContent<Intent> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent.DownloadTriggerEvent> = consumed(),
    val snackbarMessageContent: StateEventWithContent<String> = consumed(),
    val shouldShowAdsForLink: Boolean = false
)