package mega.privacy.android.app.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.listeners.ExportListener
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.DrawerItem
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object MegaNodeUtil {
    /**
     * alertTakenDown is the dialog to be shown. It resides inside this static class to prevent multiple definition within the activity class
     */
    private var alertTakenDown: AlertDialog? = null

    /**
     * The method to calculate how many nodes are folders in array list
     *
     * @param nodes the nodes to be calculated
     * @return how many nodes are folders in array list
     */
    @JvmStatic
    fun getNumberOfFolders(nodes: ArrayList<MegaNode?>?): Int {
        if (nodes == null) {
            return 0
        }

        var folderCount = 0
        val safeList = CopyOnWriteArrayList(nodes)
        for (node in safeList) {
            if (node == null) {
                safeList.remove(node)
            } else if (node.isFolder) {
                folderCount++
            }
        }

        return folderCount
    }

    /**
     * @param node the detected node
     * @return whether the node is taken down
     */
    fun isNodeTakenDown(node: MegaNode?): Boolean {
        return node != null && node.isTakenDown
    }

    /**
     * If the node is taken down, and try to execute action against the node,
     * such as manage link, remove link, show the alert dialog
     *
     * @param node the detected node
     * @return whether show the dialog for the mega node or not
     */
    @JvmStatic
    fun showTakenDownNodeActionNotAvailableDialog(node: MegaNode?, context: Context): Boolean {
        return if (isNodeTakenDown(node)) {
            Util.showSnackbar(context, getString(R.string.error_download_takendown_node))
            true
        } else {
            false
        }
    }

    /**
     * Gets the root parent folder of a node.
     *
     * @param node  MegaNode to get its root parent path
     * @return The path of the root parent of the node.
     */
    @JvmStatic
    fun getParentFolderPath(node: MegaNode?): String {
        if (node != null) {
            val megaApi = MegaApplication.getInstance().megaApi
            var rootParent = node
            while (megaApi.getParentNode(node) != null) {
                rootParent = megaApi.getParentNode(node)
            }

            val path = megaApi.getNodePath(node)
            when {
                rootParent!!.handle == megaApi.rootNode.handle -> {
                    return getString(R.string.section_cloud_drive) + path
                }
                node.handle == megaApi.rubbishNode.handle -> {
                    return getString(R.string.section_rubbish_bin) +
                            path.replace("bin" + Constants.SEPARATOR, "")
                }
                node.isInShare -> {
                    return getString(R.string.title_incoming_shares_explorer) +
                            Constants.SEPARATOR + path.substring(path.indexOf(":") + 1)
                }
            }
        }

        return ""
    }

    /**
     *
     * Shares a node.
     * If the node is a folder creates and/or shares the folder link.
     * If the node is a file and exists in local storage, shares the file. If not, creates and/or shares the file link.
     *
     * @param context   current Context.
     * @param node      node to share.
     */
    @JvmStatic
    fun shareNode(context: Context, node: MegaNode) {
        if (shouldContinueWithoutError(context, "sharing node", node)) {
            val path = FileUtil.getLocalFile(context, node.name, node.size)
            if (!TextUtil.isTextEmpty(path) && !node.isFolder) {
                FileUtil.shareFile(context, File(path))
            } else if (node.isExported) {
                startShareIntent(context, Intent(Intent.ACTION_SEND), node.publicLink)
            } else {
                MegaApplication.getInstance().megaApi.exportNode(
                    node, ExportListener(context, Intent(Intent.ACTION_SEND))
                )
            }
        }
    }

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
    @JvmStatic
    fun shareNodes(context: Context, nodes: List<MegaNode>) {
        if (!shouldContinueWithoutError(context, "sharing nodes", nodes)) {
            return
        }

        val downloadedFiles: MutableList<File> = ArrayList()
        var allDownloadedFiles = true
        for (node in nodes) {
            val path =
                if (node.isFolder) null else FileUtil.getLocalFile(context, node.name, node.size)
            if (TextUtil.isTextEmpty(path)) {
                allDownloadedFiles = false
                break
            } else {
                downloadedFiles.add(File(path!!))
            }
        }

        if (allDownloadedFiles) {
            FileUtil.shareFiles(context, downloadedFiles)
            return
        }

        var notExportedNodes = 0
        val links = StringBuilder()
        for (node in nodes) {
            if (!node.isExported) {
                notExportedNodes++
            } else {
                links.append(node.publicLink)
                    .append("\n\n")
            }
        }

        if (notExportedNodes == 0) {
            startShareIntent(
                context, Intent(Intent.ACTION_SEND),
                links.toString()
            )
            return
        }

        val megaApi = MegaApplication.getInstance().megaApi
        val exportListener = ExportListener(
            context, notExportedNodes, links,
            Intent(Intent.ACTION_SEND)
        )

        for (node in nodes) {
            if (!node.isExported) {
                megaApi.exportNode(node, exportListener)
            }
        }
    }

    /**
     * Shares a link.
     *
     * @param context   current Context.
     * @param fileLink  link to share.
     */
    @JvmStatic
    fun shareLink(context: Context, fileLink: String?) {
        startShareIntent(context, Intent(Intent.ACTION_SEND), fileLink)
    }

    /**
     * Ends the creation of the share intent and starts it.
     *
     * @param context       current Context.
     * @param shareIntent   intent to start the share.
     * @param link          link of the node to share.
     */
    @JvmStatic
    fun startShareIntent(context: Context, shareIntent: Intent, link: String?) {
        shareIntent.type = Constants.TYPE_TEXT_PLAIN
        shareIntent.putExtra(Intent.EXTRA_TEXT, link)
        context.startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share)))
    }

    /**
     * Checks if there is any error before continues any action.
     *
     * @param context   current Context.
     * @param message   action being taken.
     * @param node      node involved in the action.
     * @return True if there is not any error, false otherwise.
     */
    private fun shouldContinueWithoutError(
        context: Context,
        message: String,
        node: MegaNode?
    ): Boolean {
        val error = "Error $message. "
        if (node == null) {
            LogUtil.logError(error + "Node == NULL")
            return false
        } else if (!Util.isOnline(context)) {
            LogUtil.logError(error + "No network connection")
            Util.showSnackbar(context, getString(R.string.error_server_connection_problem))
            return false
        }

        return true
    }

    /**
     * Checks if there is any error before continues any action.
     *
     * @param context   current Context.
     * @param message   action being taken.
     * @param nodes      nodes involved in the action.
     * @return True if there is not any error, false otherwise.
     */
    private fun shouldContinueWithoutError(
        context: Context, message: String,
        nodes: List<MegaNode>?
    ): Boolean {
        val error = "Error $message. "
        if (nodes == null || nodes.isEmpty()) {
            LogUtil.logError(error + "no nodes")
            return false
        } else if (!Util.isOnline(context)) {
            LogUtil.logError(error + "No network connection")
            Util.showSnackbar(context, getString(R.string.error_server_connection_problem))
            return false
        }

        return true
    }

    /**
     * Checks if a MegaNode is the user attribute "My chat files"
     *
     * @param node MegaNode to check
     * @return True if the node is "My chat files" attribute, false otherwise
     */
    private fun isMyChatFilesFolder(node: MegaNode?): Boolean {
        val megaApplication = MegaApplication.getInstance()
        return node != null && node.handle != MegaApiJava.INVALID_HANDLE &&
                !megaApplication.megaApi.isInRubbish(node) &&
                existsMyChatFilesFolder() &&
                node.handle == megaApplication.dbH.myChatFilesFolderHandle
    }

    /**
     * Checks if the user attribute "My chat files" is saved in DB and exists
     *
     * @return True if the the user attribute "My chat files" is saved in the DB, false otherwise
     */
    @JvmStatic
    fun existsMyChatFilesFolder(): Boolean {
        val dbH = MegaApplication.getInstance().dbH
        val megaApi: MegaApiJava = MegaApplication.getInstance().megaApi
        if (dbH != null && dbH.myChatFilesFolderHandle != MegaApiJava.INVALID_HANDLE) {
            val myChatFilesFolder = megaApi.getNodeByHandle(dbH.myChatFilesFolderHandle)
            return myChatFilesFolder != null &&
                    myChatFilesFolder.handle != MegaApiJava.INVALID_HANDLE &&
                    !megaApi.isInRubbish(myChatFilesFolder)
        }

        return false
    }

    /**
     * Gets the node of the user attribute "My chat files" from the DB.
     *
     * Before call this method is neccesary to call existsMyChatFilesFolder() method
     *
     * @return "My chat files" folder node
     * @see MegaNodeUtil.existsMyChatFilesFolder
     */
    @JvmStatic
    val myChatFilesFolder: MegaNode
        get() = MegaApplication.getInstance().megaApi.getNodeByHandle(MegaApplication.getInstance().dbH.myChatFilesFolderHandle)

    /**
     * Checks if a node is "Camera Uploads" or "Media Uploads" folder.
     *
     * Note: The content of this method is temporary and will have to be modified when the PR of the CU user attribute be merged.
     *
     * @param n MegaNode to check
     * @return True if the node is "Camera Uploads" or "Media Uploads" folder, false otherwise
     */
    fun isCameraUploads(n: MegaNode): Boolean {
        var cameraSyncHandle: String? = null
        var secondaryMediaHandle: String? = null
        val dbH = MegaApplication.getInstance().dbH
        val prefs = dbH.preferences

        //Check if the item is the Camera Uploads folder
        if (prefs != null && prefs.camSyncHandle != null) {
            cameraSyncHandle = prefs.camSyncHandle
        }

        val handle = n.handle
        if (cameraSyncHandle != null && cameraSyncHandle.isNotEmpty()
            && handle == cameraSyncHandle.toLong() && !isNodeInRubbishOrDeleted(handle)
        ) {
            return true
        }

        //Check if the item is the Media Uploads folder
        if (prefs != null && prefs.megaHandleSecondaryFolder != null) {
            secondaryMediaHandle = prefs.megaHandleSecondaryFolder
        }

        return (secondaryMediaHandle != null && secondaryMediaHandle.isNotEmpty()
                && handle == secondaryMediaHandle.toLong() && !isNodeInRubbishOrDeleted(handle))
    }

    /**
     * Checks if a node is  outgoing or a pending outgoing share.
     *
     * @param node MegaNode to check
     * @return True if the node is a outgoing or a pending outgoing share, false otherwise
     */
    @JvmStatic
    fun isOutShare(node: MegaNode): Boolean {
        return node.isOutShare || MegaApplication.getInstance().megaApi.isPendingShare(node)
    }

    /**
     * Gets the the icon that has to be displayed for a folder.
     *
     * @param node          MegaNode referencing the folder to check
     * @param drawerItem    indicates if the icon has to be shown in Outgoing shares section or any other
     * @return The icon of the folder to be displayed.
     */
    @JvmStatic
    fun getFolderIcon(node: MegaNode, drawerItem: DrawerItem): Int {
        return if (node.isInShare) {
            R.drawable.ic_folder_incoming
        } else if (isCameraUploads(node)) {
            if (drawerItem == DrawerItem.SHARED_ITEMS && isOutShare(node)) {
                R.drawable.ic_folder_outgoing
            } else {
                R.drawable.ic_folder_camera_uploads_list
            }
        } else if (isMyChatFilesFolder(node)) {
            if (drawerItem == DrawerItem.SHARED_ITEMS && isOutShare(node)) {
                R.drawable.ic_folder_outgoing
            } else {
                R.drawable.ic_folder_chat_list
            }
        } else if (isOutShare(node)) {
            R.drawable.ic_folder_outgoing
        } else {
            R.drawable.ic_folder_list
        }
    }

    /**
     * Gets the parent MegaNode of the highest level in tree of the node passed by param.
     *
     * @param node  MegaNode to check
     * @return The root parent MegaNode
     */
    @JvmStatic
    fun getRootParentNode(node: MegaNode): MegaNode {
        val megaApi = MegaApplication.getInstance().megaApi
        var rootParent = node
        while (megaApi.getParentNode(node) != null) {
            rootParent = megaApi.getParentNode(node)
        }

        return rootParent
    }

    /**
     * Checks if it is on Links section and in root level.
     *
     * @param adapterType   current section
     * @param parentHandle  current parent handle
     * @return true if it is on Links section and it is in root level, false otherwise
     */
    @JvmStatic
    fun isInRootLinksLevel(adapterType: Int, parentHandle: Long): Boolean {
        return adapterType == Constants.LINKS_ADAPTER && parentHandle == MegaApiJava.INVALID_HANDLE
    }

    /*
     * Checks if the Toolbar option "share" should be visible or not depending on the permissions of the MegaNode
     *
     * @param adapterType   view in which is required the check
     * @param isFolderLink  if true, the node comes from a folder link
     * @param handle        identifier of the MegaNode to check
     * @return True if the option "share" should be visible, false otherwise
     */
    @JvmStatic
    fun showShareOption(adapterType: Int, isFolderLink: Boolean, handle: Long): Boolean {
        if (isFolderLink) {
            return false
        } else if (adapterType != Constants.OFFLINE_ADAPTER &&
            adapterType != Constants.ZIP_ADAPTER && adapterType != Constants.FILE_LINK_ADAPTER
        ) {
            val megaApi = MegaApplication.getInstance().megaApi
            val node = megaApi.getNodeByHandle(handle)
            return node != null && megaApi.getAccess(node) == MegaShare.ACCESS_OWNER
        }

        return true
    }

    /**
     * This method is to detect whether the node exist and in rubbish bean
     * @param handle node's handle to be detected
     * @return whether the node is in rubbish
     */
    @JvmStatic
    fun isNodeInRubbish(handle: Long): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi
        val node = megaApi.getNodeByHandle(handle)
        return node != null && megaApi.isInRubbish(node)
    }

    /**
     * This method is to detect whether the node has been deleted completely
     * or in rubbish bin
     * @param handle node's handle to be detected
     * @return whether the node is in rubbish
     */
    @JvmStatic
    fun isNodeInRubbishOrDeleted(handle: Long): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi
        val node = megaApi.getNodeByHandle(handle)
        return node == null || megaApi.isInRubbish(node)
    }

    /**
     * Gets the parent outgoing or incoming MegaNode folder of a node.
     *
     * @param node  MegaNode to get its parent
     * @return The outgoing or incoming parent folder.
     */
    @JvmStatic
    fun getOutgoingOrIncomingParent(node: MegaNode): MegaNode? {
        if (isOutgoingOrIncomingFolder(node)) {
            return node
        }

        var parentNode = node
        val megaApi: MegaApiJava = MegaApplication.getInstance().megaApi
        while (megaApi.getParentNode(parentNode) != null) {
            parentNode = megaApi.getParentNode(parentNode)
            if (isOutgoingOrIncomingFolder(parentNode)) {
                return parentNode
            }
        }

        return null
    }

    /**
     * Checks if a node is an outgoing or an incoming folder.
     *
     * @param node  MegaNode to check
     * @return  True if the node is an outgoing or incoming folder, false otherwise.
     */
    private fun isOutgoingOrIncomingFolder(node: MegaNode): Boolean {
        return node.isOutShare || node.isInShare
    }

    /*
     * Check if all nodes can be moved to rubbish bin.
     *
     * @param nodes nodes to check
     * @return whether all nodes can be moved to rubbish bin
     */
    @JvmStatic
    fun canMoveToRubbish(nodes: List<MegaNode?>): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi
        for (node in nodes) {
            if (megaApi.checkMove(node, megaApi.rubbishNode).errorCode != MegaError.API_OK) {
                return false
            }
        }

        return true
    }

    /**
     * Check if all nodes are file nodes.
     *
     * @param nodes nodes to check
     * @return whether all nodes are file nodes
     */
    @JvmStatic
    fun areAllFileNodes(nodes: List<MegaNode>): Boolean {
        for (node in nodes) {
            if (!node.isFile) {
                return false
            }
        }

        return true
    }

    /**
     * Check if all nodes have full access.
     *
     * @param nodes nodes to check
     * @return whether all nodes have full access
     */
    @JvmStatic
    fun allHaveFullAccess(nodes: List<MegaNode?>): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi
        for (node in nodes) {
            if (megaApi.checkAccess(node, MegaShare.ACCESS_FULL).errorCode != MegaError.API_OK) {
                return false
            }
        }

        return true
    }

    /**
     * Shows a confirmation warning before leave an incoming share.
     *
     * @param context   current Context
     * @param n         incoming share to leave
     */
    @JvmStatic
    fun showConfirmationLeaveIncomingShare(context: Context, n: MegaNode?) {
        showConfirmationLeaveIncomingShares(context, n, null)
    }

    /**
     * Shows a confirmation warning before leave some incoming shares.
     *
     * @param context       current Context
     * @param handleList    handles list of the incoming shares to leave
     */
    @JvmStatic
    fun showConfirmationLeaveIncomingShares(context: Context, handleList: ArrayList<Long>?) {
        showConfirmationLeaveIncomingShares(context, null, handleList)
    }

    /**
     * Shows a confirmation warning before leave one or more incoming shares.
     *
     * @param context       current Context
     * @param n             if only one incoming share to leave, its node, null otherwise
     * @param handleList    if mode than one incoming shares to leave, list of its handles, null otherwise
     */
    private fun showConfirmationLeaveIncomingShares(
        context: Context,
        n: MegaNode?,
        handleList: ArrayList<Long>?
    ) {
        val onlyOneIncomingShare = n != null && handleList == null
        val numIncomingShares = if (onlyOneIncomingShare) 1 else handleList!!.size
        val builder = MaterialAlertDialogBuilder(context)
        builder.setMessage(
            getQuantityString(R.plurals.confirmation_leave_share_folder, numIncomingShares)
        )
            .setPositiveButton(getString(R.string.general_leave)) { _, _ ->
                if (onlyOneIncomingShare) {
                    NodeController(context).leaveIncomingShare(n)
                } else {
                    NodeController(context).leaveMultipleIncomingShares(handleList)
                }
                MegaApplication.getInstance()
                    .sendBroadcast(Intent(BroadcastConstants.BROADCAST_ACTION_DESTROY_ACTION_MODE))
            }
            .setNegativeButton(getString(R.string.general_cancel), null)
            .show()
    }

    /**
     * Checks if a folder node is empty.
     * If a folder is empty means although contains more folders inside,
     * all of them don't contain any file.
     *
     * @param node  MegaNode to check.
     * @return  True if the folder is folder and is empty, false otherwise.
     */
    @JvmStatic
    fun isEmptyFolder(node: MegaNode?): Boolean {
        if (node == null || node.isFile) {
            return false
        }

        val megaApi = MegaApplication.getInstance().megaApi
        val children: List<MegaNode?>? = megaApi.getChildren(node)
        if (children != null && children.isNotEmpty()) {
            for (child in children) {
                if (child == null) {
                    continue
                }

                if (child.isFile || !isEmptyFolder(child)) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Get list of all child files.
     *
     * @param megaApi MegaApiAndroid instance
     * @param dlFiles map to store all child files
     * @param parent the parent node
     * @param folder the destination folder
     */
    @JvmStatic
    fun getDlList(
        megaApi: MegaApiAndroid, dlFiles: MutableMap<MegaNode, String>,
        parent: MegaNode?, folder: File
    ) {
        if (megaApi.rootNode == null) {
            return
        }

        val nodeList = megaApi.getChildren(parent)
        if (nodeList.size == 0) {
            // if this is an empty folder, do nothing
            return
        }

        folder.mkdir()
        for (i in nodeList.indices) {
            val document = nodeList[i]
            if (document.type == MegaNode.TYPE_FOLDER) {
                val subfolder = File(folder, document.name)
                getDlList(megaApi, dlFiles, document, subfolder)
            } else {
                dlFiles[document] = folder.absolutePath
            }
        }
    }

    /**
     * Gets the tinted circle Drawable for the provided [MegaNode] Label
     *
     * @param nodeLabel     [MegaNode] Label
     * @param resources     Android resources
     * @return              Drawable
     */
    @JvmStatic
    fun getNodeLabelDrawable(nodeLabel: Int, resources: Resources): Drawable? {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_circle_label, null)
        drawable?.setTint(ResourcesCompat.getColor(resources, getNodeLabelColor(nodeLabel), null))
        return drawable
    }

    /**
     * Gets the String resource reference for the provided [MegaNode] Label
     *
     * @param nodeLabel     [MegaNode] Label
     * @return              String resource reference
     */
    @JvmStatic
    fun getNodeLabelText(nodeLabel: Int): String? = getString(
        when (nodeLabel) {
            MegaNode.NODE_LBL_RED -> R.string.label_red
            MegaNode.NODE_LBL_ORANGE -> R.string.label_orange
            MegaNode.NODE_LBL_YELLOW -> R.string.label_yellow
            MegaNode.NODE_LBL_GREEN -> R.string.label_green
            MegaNode.NODE_LBL_BLUE -> R.string.label_blue
            MegaNode.NODE_LBL_PURPLE -> R.string.label_purple
            else -> R.string.label_grey
        }
    )

    /**
     * Gets the Color resource reference for the provided [MegaNode] Label
     *
     * @param nodeLabel     [MegaNode] Label
     * @return              Color resource reference
     */
    @JvmStatic
    @ColorRes
    fun getNodeLabelColor(nodeLabel: Int): Int {
        return when (nodeLabel) {
            MegaNode.NODE_LBL_RED -> R.color.salmon_400_salmon_300
            MegaNode.NODE_LBL_ORANGE -> R.color.orange_400_orange_300
            MegaNode.NODE_LBL_YELLOW -> R.color.yellow_600_yellow_300
            MegaNode.NODE_LBL_GREEN -> R.color.green_400_green_300
            MegaNode.NODE_LBL_BLUE -> R.color.blue_300_blue_200
            MegaNode.NODE_LBL_PURPLE -> R.color.purple_300_purple_200
            else -> R.color.grey_300
        }
    }

    /**
     * Gets the handle of Cloud root node.
     *
     * @return The handle of Cloud root node if available, invalid handle otherwise.
     */
    @JvmStatic
    val cloudRootHandle: Long
        get() {
            val rootNode = MegaApplication.getInstance().megaApi.rootNode
            return rootNode?.handle ?: MegaApiJava.INVALID_HANDLE
        }

    /**
     * Setup SDK HTTP streaming server.
     *
     * @param api MegaApiAndroid instance to use
     * @param context Android context
     */
    fun setupStreamingServer(api: MegaApiAndroid, context: Context) {
        if (api.httpServerIsRunning() == 0) {
            api.httpServerStart()
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            if (memoryInfo.totalMem > Constants.BUFFER_COMP) {
                api.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
            } else {
                api.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
            }
        }
    }

    /**
     * Shows a taken down alert.
     *
     * @param activity the activity is the page where dialog is shown
     */
    @JvmStatic
    fun showTakenDownAlert(activity: AppCompatActivity?) {
        if (activity == null || activity.isFinishing
            || alertTakenDown != null && alertTakenDown!!.isShowing
        ) {
            return
        }
        val dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setTitle(getString(R.string.general_not_available))
            .setMessage(getString(R.string.error_download_takendown_node))
            .setNegativeButton(getString(R.string.general_dismiss)) { _, _ -> activity.finish() }
        alertTakenDown = dialogBuilder.create()
        alertTakenDown!!.setCancelable(false)
        alertTakenDown!!.show()
    }

    /**
     * show dialog
     *
     * @param isFolder        the clicked node
     * @param currentPosition the view position in adapter
     * @param listener        the listener to handle all clicking event
     * @param context         the context where adapter resides
     * @return the dialog object to be handled by adapter to be dismissed, in case of window leaking situation
     */
    @JvmStatic
    fun showTakenDownDialog(
        isFolder: Boolean,
        currentPosition: Int,
        listener: NodeTakenDownDialogListener,
        context: Context
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.dialog_three_vertical_buttons, null)

        builder.setView(v)

        val title = v.findViewById<TextView>(R.id.dialog_title)
        val text = v.findViewById<TextView>(R.id.dialog_text)
        val openButton = v.findViewById<Button>(R.id.dialog_first_button)
        val disputeButton = v.findViewById<Button>(R.id.dialog_second_button)
        val cancelButton = v.findViewById<Button>(R.id.dialog_third_button)

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.END

        title.text = getString(R.string.general_error_word)
        text.text =
            getString(if (isFolder) R.string.message_folder_takedown_pop_out_notification else R.string.message_file_takedown_pop_out_notification)
        openButton.text = getString(R.string.context_open_link)
        disputeButton.text = getString(R.string.dispute_takendown_file)
        cancelButton.text = getString(R.string.general_cancel)

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        openButton.setOnClickListener {
            listener.onOpenClicked(currentPosition)
            dialog.dismiss()
        }

        disputeButton.setOnClickListener {
            listener.onDisputeClicked()
            val openTermsIntent = Intent(context, WebViewActivity::class.java)
            openTermsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            openTermsIntent.data = Uri.parse(Constants.DISPUTE_URL)
            context.startActivity(openTermsIntent)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            listener.onCancelClicked()
            dialog.dismiss()
        }

        dialog.show()

        return dialog
    }
}
