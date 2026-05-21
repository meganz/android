package mega.privacy.android.app.utils.wrapper

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.utils.NodeTakenDownDialogListener
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.io.File

interface MegaNodeUtilWrapper {

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
     * Check if all nodes are file nodes and not taken down.
     *
     * @param nodes nodes to check
     * @return whether all nodes are file nodes and not taken down.
     */
    fun areAllFileNodesAndNotTakenDown(nodes: List<MegaNode>): Boolean

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
    fun getNodeLabelText(nodeLabel: Int, context: Context): String?

    /**
     * Gets the Color resource reference for the provided [MegaNode] Label
     *
     * @param nodeLabel     [MegaNode] Label
     * @return              Color resource reference
     */
    @ColorRes
    fun getNodeLabelColor(nodeLabel: Int): Int

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
     * @param listener        the listener to handle all clicking event
     * @param context         the context where adapter resides
     * @return the dialog object to be handled by adapter to be dismissed, in case of window leaking situation
     */
    fun showTakenDownDialog(
        isFolder: Boolean,
        listener: NodeTakenDownDialogListener? = null,
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
    fun getFileInfo(node: MegaNode, context: Context): String?

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
     * Setup streaming server
     *
     */
    fun setupStreamingServer()
}
