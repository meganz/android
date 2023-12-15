package mega.privacy.android.app.presentation.folderlink.model

import android.content.Intent
import androidx.annotation.StringRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.myAccount.StorageStatusDialogState
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.folderlink.FolderLinkViewModel]
 *
 * @property isInitialState             Whether it is initial state
 * @property url                        Url of the folder
 * @property folderSubHandle            Handle of the folder link
 * @property isLoginComplete            Whether is login is successfully completed
 * @property isNodesFetched             Whether nodes are fetched
 * @property askForDecryptionKeyDialog  Whether to show AskForDecryptionDialog
 * @property collisions                 List of nodes with existing names
 * @property copyResultText             Text to show on successful copy
 * @property copyThrowable              Throwable error on copy
 * @property shouldLogin                Whether to show login screen
 * @property hasDbCredentials           Whether db credentials are valid
 * @property hasMediaItem               Whether current folder has any image/video
 * @property nodesList                  List of nodes to show
 * @property rootNode                   Root node
 * @property parentNode                 Parent node of the nodes shown
 * @property currentViewType            Whether list or grid view
 * @property title                      Title of the folder
 * @property selectedNodeCount          Count of nodes selected
 * @property finishActivity             Whether to finish the activity
 * @property openFile                   State to handle file opening
 * @property downloadNodes              State to download nodes
 * @property downloadEvent              Event to download nodes with DownloadsWorker
 * @property importNode                 Node to import
 * @property selectImportLocation       State to open location selection
 * @property snackbarMessageContent     State to show snackbar message
 * @property openMoreOption             State to open more option bottom sheet
 * @property moreOptionNode             Node to show more options for
 * @property storageStatusDialogState   State of StorageStatusDialog
 * @property errorDialogTitle           String id of title for error dialog
 * @property errorDialogContent         String id of content for error dialog
 * @property snackBarMessage            String id of content for snack bar
 */
data class FolderLinkState(
    val isInitialState: Boolean = true,
    val url: String? = null,
    val folderSubHandle: String? = null,
    val isLoginComplete: Boolean = false,
    val isNodesFetched: Boolean = false,
    val askForDecryptionKeyDialog: Boolean = false,
    val collisions: ArrayList<NameCollision>? = null,
    val copyThrowable: Throwable? = null,
    val copyResultText: String? = null,
    val shouldLogin: Boolean? = null,
    val hasDbCredentials: Boolean = false,
    val hasMediaItem: Boolean = false,
    val nodesList: List<NodeUIItem<TypedNode>> = listOf(),
    val rootNode: TypedFolderNode? = null,
    val parentNode: TypedFolderNode? = null,
    val currentViewType: ViewType = ViewType.LIST,
    val title: String = "",
    val selectedNodeCount: Int = 0,
    val finishActivity: Boolean = false,
    val openFile: StateEventWithContent<Intent> = consumed(),
    val downloadNodes: StateEventWithContent<List<MegaNode>> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val importNode: NodeUIItem<TypedNode>? = null,
    val selectImportLocation: StateEvent = consumed,
    val snackbarMessageContent: StateEventWithContent<String> = consumed(),
    val openMoreOption: StateEvent = consumed,
    val moreOptionNode: NodeUIItem<TypedNode>? = null,
    val storageStatusDialogState: StorageStatusDialogState? = null,
    @StringRes val errorDialogTitle: Int = -1,
    @StringRes val errorDialogContent: Int = -1,
    @StringRes val snackBarMessage: Int = -1,
)