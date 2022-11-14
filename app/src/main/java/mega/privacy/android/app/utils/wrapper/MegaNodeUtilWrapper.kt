package mega.privacy.android.app.utils.wrapper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import mega.privacy.android.app.components.saver.AutoPlayInfo
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.NodeTakenDownDialogListener
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.io.File

interface MegaNodeUtilWrapper {


    /**
     * Initialise this component
     *
     */
    fun observeBackupFolder()

    /**
     * Gets the node of the user attribute "My chat files" from the DB.
     *
     * Before call this method is necessary to call existsMyChatFilesFolder() method
     *
     * @return "My chat files" folder node
     * @see MegaNodeUtil.existsMyChatFilesFolder
     */
    fun getMyChatFilesFolder(): MegaNode

    /**
     * Gets the handle of Cloud root node.
     *
     * @return The handle of Cloud root node if available, invalid handle otherwise.
     */
    fun getCloudRootHandle(): Long

    /**
     * The method to calculate how many nodes are folders in array list
     *
     * @param nodes the nodes to be calculated
     * @return how many nodes are folders in array list
     */
    fun getNumberOfFolders(nodes: List<MegaNode?>?): Int

    /**
     * If the node is taken down, and try to execute action against the node,
     * such as manage link, remove link, show the alert dialog
     *
     * @param node the detected node
     * @return whether show the dialog for the mega node or not
     */
    fun showTakenDownNodeActionNotAvailableDialog(node: MegaNode?, context: Context): Boolean

    /**
     * Gets the path of a folder.
     *
     * @param nodeFolder  MegaNode to get its path
     * @return The path of the of the folder.
     */
    fun getNodeFolderPath(nodeFolder: MegaNode?): String

    /**
     *
     * Shares a node.
     *
     * @param context Current Context.
     * @param node    Node to share.
     */
    fun shareNode(context: Context, node: MegaNode?)

    /**
     *
     * Shares a node.
     * If the node is a folder creates and/or shares the folder link.
     * If the node is a file and exists in local storage, shares the file. If not, creates and/or shares the file link.
     *
     * @param context                  Current Context.
     * @param node                     Node to share.
     * @param onExportFinishedListener Listener to manage the result of export request.
     */
    fun shareNode(
        context: Context,
        node: MegaNode?,
        onExportFinishedListener: (() -> Unit)?,
    )

    /**
     * Method to know if all nodes are unloaded. If so, share them.
     *
     * @param context   The Activity context.
     * @param listNodes The list of nodes to be checked.
     * @return True, if all are downloaded. False, otherwise.
     */
    fun areAllNodesDownloaded(context: Context, listNodes: List<MegaNode>): Boolean

    /**
     * Method to get the link to the exported nodes.
     *
     * @param listNodes The list of nodes to be checked.
     * @return The link with all exported nodes
     */
    fun getExportNodesLink(listNodes: List<MegaNode>): StringBuilder

    /**
     * Share multiple nodes out of MEGA app.
     *
     * If a folder is involved, we will share links of all nodes.
     *
     * Other apps can't handle the mixture of link and file, so if there is any file that is not
     * downloaded, we will share links of all files.
     *
     * @param context the context where nodes are shared
     * @param nodes nodes to share
     */
    fun shareNodes(context: Context, nodes: List<MegaNode>)

    /**
     * Shares a link.
     *
     * @param context   current Context.
     * @param fileLink  link to share.
     */
    fun shareLink(context: Context, fileLink: String?)

    /**
     * Ends the creation of the share intent and starts it.
     *
     * @param context       current Context.
     * @param shareIntent   intent to start the share.
     * @param link          link of the node to share.
     */
    fun startShareIntent(context: Context, shareIntent: Intent, link: String?)

    /**
     * Checks if there is any error before continues any action.
     *
     * @param context   current Context.
     * @param node      node involved in the action.
     * @return True if there is not any error, false otherwise.
     */
    fun shouldContinueWithoutError(
        context: Context,
        node: MegaNode?,
    ): Boolean

    /**
     * Checks if there is any error before continues any action.
     *
     * @param context   current Context.
     * @param nodes      nodes involved in the action.
     * @return True if there is not any error, false otherwise.
     */
    fun shouldContinueWithoutError(
        context: Context,
        nodes: List<MegaNode>?,
    ): Boolean

    /**
     * Checks if the user attribute "My chat files" is saved in DB and exists
     *
     * @return True if the the user attribute "My chat files" is saved in the DB, false otherwise
     */
    fun existsMyChatFilesFolder(): Boolean

    /**
     * Checks if a node is  outgoing or a pending outgoing share.
     *
     * @param node MegaNode to check
     * @return True if the node is a outgoing or a pending outgoing share, false otherwise
     */
    fun isOutShare(node: MegaNode): Boolean

    /**
     * Gets the the icon that has to be displayed for a folder.
     *
     * @param node          MegaNode referencing the folder to check
     * @param drawerItem    indicates if the icon has to be shown in Outgoing shares section or any other
     * @return The icon of the folder to be displayed.
     */
    fun getFolderIcon(node: MegaNode, drawerItem: DrawerItem): Int

    /**
     * Checks if it is on Links section and in root level.
     *
     * @param adapterType   current section
     * @param parentHandle  current parent handle
     * @return true if it is on Links section and it is in root level, false otherwise
     */
    fun isInRootLinksLevel(adapterType: Int, parentHandle: Long): Boolean

    /**
     * Checks if the Toolbar option "share" should be visible or not depending on the permissions of the MegaNode
     *
     * @param adapterType   view in which is required the check
     * @param isFolderLink  if true, the node comes from a folder link
     * @param handle        identifier of the MegaNode to check
     * @return True if the option "share" should be visible, false otherwise
     */
    fun showShareOption(adapterType: Int, isFolderLink: Boolean, handle: Long): Boolean

    /**
     * This method is to detect whether the node has been deleted completely
     * or in rubbish bin
     * @param handle node's handle to be detected
     * @return whether the node is in rubbish
     */
    fun isNodeInRubbishOrDeleted(handle: Long): Boolean

    /**
     * Check if all nodes can be moved to rubbish bin.
     *
     * @param nodes nodes to check
     * @return whether all nodes can be moved to rubbish bin
     */
    fun canMoveToRubbish(nodes: List<MegaNode?>): Boolean

    /**
     * Check if all nodes are file nodes and not taken down.
     *
     * @param nodes nodes to check
     * @return whether all nodes are file nodes and not taken down.
     */
    fun areAllFileNodesAndNotTakenDown(nodes: List<MegaNode>): Boolean

    /**
     * Check if all nodes have full access.
     *
     * @param nodes nodes to check
     * @return whether all nodes have full access
     */
    fun allHaveFullAccess(nodes: List<MegaNode?>): Boolean

    /**
     * Check if all nodes have owner access and are not taken down.
     *
     * @param nodes List of nodes to check.
     * @return True if all nodes have owner access and are not taken down, false otherwise.
     */
    fun allHaveOwnerAccessAndNotTakenDown(nodes: List<MegaNode?>): Boolean

    /**
     * Shows a confirmation warning before leave an incoming share.
     *
     * @param activity current Activity
     * @param snackbarShower interface to show snackbar
     * @param node incoming share to leave
     */
    fun showConfirmationLeaveIncomingShare(
        activity: Activity,
        snackbarShower: SnackbarShower,
        node: MegaNode,
    )

    /**
     * Shows a confirmation warning before leave some incoming shares.
     *
     * @param activity current Activity
     * @param snackbarShower interface to show snackbar
     * @param handleList    handles list of the incoming shares to leave
     */
    fun showConfirmationLeaveIncomingShares(
        activity: Activity,
        snackbarShower: SnackbarShower,
        handleList: ArrayList<Long>,
    )

    /**
     * Checks if a folder node is empty.
     * If a folder is empty means although contains more folders inside,
     * all of them don't contain any file.
     *
     * @param node  MegaNode to check.
     * @return  True if the folder is folder and is empty, false otherwise.
     */
    fun isEmptyFolder(node: MegaNode?): Boolean

    /**
     * Get list of all child files.
     *
     * @param megaApi MegaApiAndroid instance
     * @param dlFiles map to store all child files
     * @param parent the parent node
     * @param folder the destination folder
     */
    fun getDlList(
        megaApi: MegaApiAndroid, dlFiles: MutableMap<MegaNode, String>,
        parent: MegaNode?, folder: File,
    )

    /**
     * Gets the tinted circle Drawable for the provided [MegaNode] Label
     *
     * @param nodeLabel     [MegaNode] Label
     * @param resources     Android resources
     * @return              Drawable
     */
    fun getNodeLabelDrawable(nodeLabel: Int, resources: Resources): Drawable?

    /**
     * Gets the String resource reference for the provided [MegaNode] Label
     *
     * @param nodeLabel     [MegaNode] Label
     * @return              String resource reference
     */
    fun getNodeLabelText(nodeLabel: Int): String?

    /**
     * Gets the Color resource reference for the provided [MegaNode] Label
     *
     * @param nodeLabel     [MegaNode] Label
     * @return              Color resource reference
     */
    @ColorRes
    fun getNodeLabelColor(nodeLabel: Int): Int

    /**
     * Setup SDK HTTP streaming server.
     *
     * @param api MegaApiAndroid instance to use
     * @param context Android context
     * @return whether this function call really starts SDK HTTP streaming server
     */
    fun setupStreamingServer(api: MegaApiAndroid, context: Context): Boolean

    /**
     * Stop SDK HTTP streaming server.
     *
     * @param shouldStopServer True if should stop the server, false otherwise.
     * @param megaApi          MegaApiAndroid instance to use.
     */
    fun stopStreamingServerIfNeeded(shouldStopServer: Boolean, megaApi: MegaApiAndroid)

    /**
     * show dialog
     *
     * @param isFolder        the clicked node
     * @param currentPosition the view position in adapter
     * @param listener        the listener to handle all clicking event
     * @param context         the context where adapter resides
     * @return the dialog object to be handled by adapter to be dismissed, in case of window leaking situation
     */
    fun showTakenDownDialog(
        isFolder: Boolean,
        currentPosition: Int,
        listener: NodeTakenDownDialogListener,
        context: Context,
    ): AlertDialog

    /**
     * Start [FileExplorerActivity] to select folder to move nodes.
     *
     * @param activity current Android activity
     * @param handles handles to move
     */
    fun selectFolderToMove(activity: Activity, handles: LongArray)

    /**
     * Start [FileExplorerActivity] to select folder to copy nodes.
     *
     * @param activity current Android activity
     * @param handles handles to copy
     */
    fun selectFolderToCopy(activity: Activity, handles: LongArray)

    /**
     * Get location info of a node.
     *
     * @param adapterType node source adapter type
     * @param fromIncomingShare is from incoming share
     * @param handle node handle
     *
     * @return location info
     */
    fun getNodeLocationInfo(
        adapterType: Int,
        fromIncomingShare: Boolean,
        handle: Long,
    ): LocationInfo?

    /**
     * Handle click event of the location text.
     *
     * @param activity current activity
     * @param adapterType node source adapter type
     * @param location location info
     */
    fun handleLocationClick(activity: Activity, adapterType: Int, location: LocationInfo)

    /**
     * Auto play a node when it's downloaded.
     *
     * @param context Android context
     * @param autoPlayInfo auto play info
     * @param activityLauncher interface to launch activity
     * @param snackbarShower interface to show snackbar
     */
    fun autoPlayNode(
        context: Context,
        autoPlayInfo: AutoPlayInfo,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
    )

    /**
     * Launch [ZipBrowserActivity] to preview a zip file.
     *
     * @param context Android context.
     * @param activityLauncher interface to launch activity.
     * @param zipFilePath The local path of the zip file.
     * @param snackbarShower interface to snackbar shower
     * @param nodeHandle The handle of the corresponding node.
     */
    fun openZip(
        context: Context,
        activityLauncher: ActivityLauncher,
        zipFilePath: String,
        snackbarShower: SnackbarShower,
        nodeHandle: Long,
    )

    /**
     * For the node that cannot be opened in-app.
     * Launch an intent with ACTION_VIEW and let user choose to use which app to open it.
     *
     * @param context Android context
     * @param nodeName Name of the node.
     * @param localPath Local path of the node.
     * @param activityLauncher interface to launch activity
     * @param snackbarShower interface to show snackbar
     */
    fun launchActionView(
        context: Context,
        nodeName: String,
        localPath: String,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
    )

    /**
     * Gets the string to show as file info details with the next format: "size · modification date".
     *
     * @param node The file node from which to get the details.
     * @return The string so show as file info details.
     */
    fun getFileInfo(node: MegaNode): String?

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context     Current context.
     * @param node        Node to preview on Text Editor.
     * @param adapterType Current adapter view.
     */
    fun manageTextFileIntent(context: Context, node: MegaNode, adapterType: Int)

    /**
     * Launches an Intent to open TextFileEditorActivity on edit mode.
     *
     * @param context     Current context.
     * @param node        Node to preview on Text Editor.
     * @param adapterType Current adapter view.
     */
    fun manageEditTextFileIntent(context: Context, node: MegaNode, adapterType: Int)

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context     Current context.
     * @param node        Node to preview on Text Editor.
     * @param adapterType Current adapter view.
     * @param urlFileLink Link of the file if the adapter is FILE_LINK_ADAPTER.
     */
    fun manageTextFileIntent(
        context: Context,
        node: MegaNode,
        adapterType: Int,
        urlFileLink: String?,
    )

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context     Current context.
     * @param node        Node to preview on Text Editor.
     * @param adapterType Current adapter view.
     * @param urlFileLink Link of the file if the adapter is FILE_LINK_ADAPTER.
     * @param mode        Text file editor mode.
     */
    fun manageTextFileIntent(
        context: Context,
        node: MegaNode,
        adapterType: Int,
        urlFileLink: String?,
        mode: String,
    )

    /**
     * Opens an URL node.
     *
     * @param context Current context.
     * @param megaApi MegaApiAndroid instance to use.
     * @param node    MegaNode which contains an URL to open.
     */
    @Suppress("DEPRECATION")
    fun manageURLNode(context: Context, megaApi: MegaApiAndroid, node: MegaNode)

    /**
     * Handle the event when a node is tapped.
     *
     * @param context Android context
     * @param node The node tapped.
     * @param nodeDownloader Function/Methd for downloading node.
     * @param activityLauncher interface to launch activity
     * @param snackbarShower interface to show snackbar
     */

    fun onNodeTapped(
        context: Context,
        node: MegaNode,
        nodeDownloader: (node: MegaNode) -> Unit,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
    )

    /**
     * Check the folder of My Backup and get the folder node
     *
     * @param megaApi MegaApiAndroid instance to use.
     * @param handleList handles list of the nodes that selected
     * @return The node of My Backups or null
     */
    fun getBackupRootNodeByHandle(
        megaApi: MegaApiAndroid,
        handleList: ArrayList<Long>?,
    ): MegaNode?

    /**
     * Check the node type of handleList for the operation related to "My Backups"
     * if the node in the handleList is belong to "My Backups", check the type:
     * BACKUP_ROOT -> the node of "My Backups" exists in the handleList
     * BACKUP_DEVICE -> the node of BACKUP_DEVICE exists in the handleList
     * BACKUP_FOLDER -> the node of BACKUP_FOLDER exists in the handleList
     * BACKUP_FOLDER_CHILD -> the node of BACKUP_FOLDER_CHILD exists in the handleList
     * otherwise, return the type: BACKUP_NONE
     *
     * @param megaApi MegaApiAndroid instance to use.
     * @param handleList handles list of the nodes that selected
     *
     * @return The type of handleList
     * if multiple nodes selected and MyBackup folder included, return BACKUP_ROOT
     * if multiple nodes selected without MyBackup folder, return BACKUP_NONE
     */
    fun checkBackupNodeTypeInList(megaApi: MegaApiAndroid, handleList: List<Long>?): Int

    /**
     * Check the type of node for the operation related to "My Backups"
     * if the node is belong to "My Backups", check the type:
     * BACKUP_ROOT -> the node of "My Backups"
     * BACKUP_DEVICE -> child node of "My Backups"
     * BACKUP_FOLDER -> child node of BACKUP_DEVICE
     * BACKUP_FOLDER_CHILD -> child node of BACKUP_FOLDER
     * otherwise, return the type: BACKUP_NONE
     *
     * @param megaApi MegaApiAndroid instance to use.
     * @param node The node that selected
     *
     * @return The type of MyBackup folder, if the folder is not belong to the "My Backups" folder, return BACKUP_NONE
     */
    fun checkBackupNodeTypeByHandle(megaApi: MegaApiAndroid, node: MegaNode?): Int


    /**
     * Contains media file
     *
     * @param handle
     * @return
     */
    fun containsMediaFile(handle: Long): Boolean


}