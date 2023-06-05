package mega.privacy.android.app.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.components.saver.AutoPlayInfo
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.ExportListener
import mega.privacy.android.app.listeners.RemoveListener
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.listeners.MultipleRequestListener
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.EDIT_MODE
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.MODE
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.VIEW_MODE
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_FOLDER
import mega.privacy.android.app.utils.Constants.BUFFER_COMP
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FRAGMENT_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_URL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_LOCATION_FILE_INFO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH_NAVIGATION
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.MAX_BUFFER_16MB
import mega.privacy.android.app.utils.Constants.MAX_BUFFER_32MB
import mega.privacy.android.app.utils.Constants.MULTIPLE_LEAVE_SHARE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE
import mega.privacy.android.app.utils.Constants.SEPARATOR
import mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.URL_INDICATOR
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.getTappedNodeLocalFile
import mega.privacy.android.app.utils.FileUtil.setLocalIntentParams
import mega.privacy.android.app.utils.FileUtil.shareFile
import mega.privacy.android.app.utils.FileUtil.shareFiles
import mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_DEVICE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_FOLDER_CHILD
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_ROOT
import mega.privacy.android.app.utils.TimeUtils.formatLongDateTime
import mega.privacy.android.app.utils.Util.getMediaIntent
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.Util.showSnackbar
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity
import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


object MegaNodeUtil {

    /**
     * The node handle of the "My Backup" folder if exist
     */
    @JvmField
    var myBackupHandle = INVALID_HANDLE

    /**
     * The method to calculate how many nodes are folders in array list
     *
     * @param nodes the nodes to be calculated
     * @return how many nodes are folders in array list
     */
    @JvmStatic
    fun getNumberOfFolders(nodes: List<MegaNode?>?): Int = nodes?.let {
        CopyOnWriteArrayList(it).count { node -> node?.isFolder == true }
    } ?: 0

    /**
     * @param node the detected node
     * @return whether the node is taken down
     */
    private fun isNodeTakenDown(node: MegaNode?): Boolean {
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
            RunOnUIThreadUtils.post {
                showSnackbar(context, context.getString(R.string.error_download_takendown_node))
            }
            true
        } else {
            false
        }
    }

    /**
     * Gets the path of a folder.
     *
     * @param nodeFolder  MegaNode to get its path
     * @return The path of the of the folder.
     */
    @JvmStatic
    fun getNodeFolderPath(nodeFolder: MegaNode?): String {
        if (nodeFolder == null) {
            Timber.w("Node is null, cannot get its path.")
            return ""
        }

        val app = MegaApplication.getInstance()
        val megaApi = app.megaApi
        val path = megaApi.getNodePath(nodeFolder)

        var rootParent = nodeFolder

        while (megaApi.getParentNode(rootParent) != null) {
            rootParent = megaApi.getParentNode(rootParent)
        }

        return when {
            rootParent!!.handle == megaApi.rootNode?.handle -> {
                return app.getString(R.string.section_cloud_drive) + path
            }

            rootParent.handle == megaApi.rubbishNode?.handle -> {
                return app.getString(R.string.section_rubbish_bin) +
                        path?.replace("bin$SEPARATOR", "")
            }

            nodeFolder.isInShare -> {
                return app.getString(R.string.title_incoming_shares_explorer) +
                        SEPARATOR + path?.substring(path.indexOf(":") + 1)
            }

            else -> ""
        }
    }

    /**
     *
     * Shares a node.
     *
     * @param context Current Context.
     * @param node    Node to share.
     */
    @JvmStatic
    fun shareNode(context: Context, node: MegaNode?) {
        shareNode(context, node, null)
    }

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
    @JvmStatic
    fun shareNode(
        context: Context,
        node: MegaNode?,
        onExportFinishedListener: (() -> Unit)?,
    ) {
        if (shouldContinueWithoutError(context, node)) {
            val path = getLocalFile(node)

            if (!path.isNullOrBlank() && !node.isFolder) {
                shareFile(context, File(path))
            } else if (node.isExported) {
                startShareIntent(context, Intent(Intent.ACTION_SEND), node.publicLink)
            } else {
                MegaApplication.getInstance().megaApi.exportNode(
                    node,
                    ExportListener(context, Intent(Intent.ACTION_SEND), onExportFinishedListener)
                )
            }
        }
    }

    /**
     * Method to know if all nodes are unloaded. If so, share them.
     *
     * @param context   The Activity context.
     * @param listNodes The list of nodes to be checked.
     * @return True, if all are downloaded. False, otherwise.
     */
    @JvmStatic
    fun areAllNodesDownloaded(context: Context, listNodes: List<MegaNode>): Boolean {
        val downloadedFiles = ArrayList<File>()

        for (node in listNodes) {
            val path = if (node.isFolder) null else getLocalFile(node)

            if (path.isNullOrBlank()) {
                return false
            } else {
                downloadedFiles.add(File(path))
            }
        }

        Timber.d("All nodes are downloaded, so share the files")
        shareFiles(context, downloadedFiles)

        return true
    }

    /**
     * Method to get the link to the exported nodes.
     *
     * @param listNodes The list of nodes to be checked.
     * @return The link with all exported nodes
     */
    @JvmStatic
    fun getExportNodesLink(listNodes: List<MegaNode>): StringBuilder {
        val links = StringBuilder()

        for (node in listNodes) {
            if (node.isExported) {
                links.append(node.publicLink).append("\n\n")
            }
        }

        return links
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
        if (!shouldContinueWithoutError(context, nodes)) {
            return
        }

        if (areAllNodesDownloaded(context, nodes)) {
            return
        }

        var notExportedNodes = 0
        val links = getExportNodesLink(nodes)

        for (node in nodes) {
            if (!node.isExported) {
                notExportedNodes++
            }
        }

        if (notExportedNodes == 0) {
            startShareIntent(context, Intent(Intent.ACTION_SEND), links.toString())
            return
        }

        val megaApi = MegaApplication.getInstance().megaApi
        val exportListener =
            ExportListener(context, notExportedNodes, links, Intent(Intent.ACTION_SEND))

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
        shareIntent.type = TYPE_TEXT_PLAIN
        shareIntent.putExtra(Intent.EXTRA_TEXT, link)
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.context_share)
            )
        )
    }

    /**
     * Checks if there is any error before continues any action.
     *
     * @param context   current Context.
     * @param node      node involved in the action.
     * @return True if there is not any error, false otherwise.
     */
    @OptIn(ExperimentalContracts::class)
    @JvmStatic
    fun shouldContinueWithoutError(
        context: Context,
        node: MegaNode?,
    ): Boolean {
        contract { returns(true) implies (node != null) }
        if (node == null) {
            Timber.e("Error sharing node: Node == NULL")
            return false
        } else if (!isOnline(context)) {
            Timber.e("Error sharing node: No network connection")
            showSnackbar(context, context.getString(R.string.error_server_connection_problem))
            return false
        }

        return true
    }

    /**
     * Checks if there is any error before continues any action.
     *
     * @param context   current Context.
     * @param nodes      nodes involved in the action.
     * @return True if there is not any error, false otherwise.
     */
    @JvmStatic
    fun shouldContinueWithoutError(
        context: Context,
        nodes: List<MegaNode>?,
    ): Boolean {
        if (nodes == null || nodes.isEmpty()) {
            Timber.e("Error sharing nodes: No nodes")
            return false
        } else if (!isOnline(context)) {
            Timber.e("Error sharing nodes: No network connection")
            showSnackbar(context, context.getString(R.string.error_server_connection_problem))
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

        return node != null && node.handle != INVALID_HANDLE &&
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

        if (dbH.myChatFilesFolderHandle != INVALID_HANDLE) {
            val myChatFilesFolder = megaApi.getNodeByHandle(dbH.myChatFilesFolderHandle)

            return myChatFilesFolder != null &&
                    myChatFilesFolder.handle != INVALID_HANDLE &&
                    !megaApi.isInRubbish(myChatFilesFolder)
        }

        return false
    }

    /**
     * Gets the node of the user attribute "My chat files" from the DB.
     *
     * Before call this method is necessary to call existsMyChatFilesFolder() method
     *
     * @return "My chat files" folder node
     * @see MegaNodeUtil.existsMyChatFilesFolder
     */
    @JvmStatic
    val myChatFilesFolder: MegaNode?
        get() = MegaApplication.getInstance().megaApi.getNodeByHandle(MegaApplication.getInstance().dbH.myChatFilesFolderHandle)

    /**
     * Checks if a node is "Camera Uploads" or "Media Uploads" folder.
     *
     * Note: The content of this method is temporary and will have to be modified when the PR of the CU user attribute be merged.
     *
     * @param n MegaNode to check
     * @return True if the node is "Camera Uploads" or "Media Uploads" folder, false otherwise
     */
    private fun isCameraUploads(n: MegaNode): Boolean {
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
    fun isOutShare(node: MegaNode?): Boolean {
        return node?.isOutShare == true || MegaApplication.getInstance().megaApi.isPendingShare(node)
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
        } else if (isRootBackupFolder(node)) {
            R.drawable.ic_folder_list
        } else if (isDeviceBackupFolder(node)) {
            getMyBackupSubFolderIcon(node)
        } else {
            R.drawable.ic_folder_list
        }
    }

    /**
     * Checks if a node is a device folder under the MyBackup folder.
     *
     * @param node MegaNode to check
     * @return True if the node is a device folder, false otherwise
     */
    private fun isDeviceBackupFolder(node: MegaNode?): Boolean {
        Timber.d("MyBackup + isDeviceBackupFolder node.handle = ${node?.handle}")
        return (node?.parentHandle == myBackupHandle && !node.deviceId.isNullOrBlank() && !isNodeInRubbishOrDeleted(
            node.handle
        ))
    }

    /**
     * Checks if a node is a sub-folder of MyBackup folder.
     *
     * @param node MegaNode to check
     * @return True if the node is a sub-folder of MyBackup folder, false otherwise
     */
    private fun isSubRootBackupFolder(node: MegaNode): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi
        var p = node
        if (isNodeInRubbishOrDeleted(node.handle)) {
            return false
        }

        while (megaApi.getParentNode(p) != null) {
            p = megaApi.getParentNode(p)!!
            if (p.parentHandle == myBackupHandle) {
                Timber.d("isSubRootBackupFolder")
                return true
            }
        }
        return false
    }

    /**
     * Checks if a node is the MyBackup folder.
     *
     * @param node MegaNode to check
     * @return True if the node is the MyBackup folder, false otherwise
     */
    private fun isRootBackupFolder(node: MegaNode?): Boolean {
        Timber.d("MyBackup + isRootBackupFolder node.handle = ${node?.handle}")
        return (node?.handle == myBackupHandle && !isNodeInRubbishOrDeleted(node.handle))
    }

    /**
     * Checks the type of the devices.
     *
     * @param node MegaNode to check
     * @return The resource ID
     */
    private fun getMyBackupSubFolderIcon(node: MegaNode?): Int {
        if (node?.deviceId.isNullOrBlank()) return R.drawable.ic_folder_list

        val folderName = node?.name
        return when {
            folderName?.contains(
                Regex(
                    "win|desktop",
                    RegexOption.IGNORE_CASE
                )
            ) == true -> R.drawable.pc_win

            folderName?.contains(
                Regex(
                    "linux|debian|ubuntu|centos",
                    RegexOption.IGNORE_CASE
                )
            ) == true -> R.drawable.pc_linux

            folderName?.contains(Regex("mac", RegexOption.IGNORE_CASE)) == true -> R.drawable.pc_mac
            folderName?.contains(
                Regex(
                    "ext|drive",
                    RegexOption.IGNORE_CASE
                )
            ) == true -> R.drawable.ex_drive

            else -> R.drawable.pc
        }
    }

    /**
     * Gets the parent MegaNode of the highest level in tree of the node passed by param.
     *
     * @param node  MegaNode to check
     * @return The root parent MegaNode
     */
    @JvmStatic
    fun MegaApiJava.getRootParentNode(node: MegaNode): MegaNode {
        var rootParent = node
        while (getParentNode(rootParent) != null) {
            rootParent = getParentNode(rootParent)!!
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
        return adapterType == LINKS_ADAPTER && parentHandle == INVALID_HANDLE
    }

    /**
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
        } else if (adapterType != OFFLINE_ADAPTER &&
            adapterType != ZIP_ADAPTER && adapterType != FILE_LINK_ADAPTER
        ) {
            val megaApi = MegaApplication.getInstance().megaApi
            val node = megaApi.getNodeByHandle(handle)

            return node != null && megaApi.getAccess(node) == MegaShare.ACCESS_OWNER
        }

        return true
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
     * Check if all nodes can be moved to rubbish bin.
     *
     * @param nodes nodes to check
     * @return whether all nodes can be moved to rubbish bin
     */
    @JvmStatic
    fun canMoveToRubbish(nodes: List<MegaNode?>): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi

        for (node in nodes) {
            if (megaApi.checkMoveErrorExtended(
                    node,
                    megaApi.rubbishNode
                ).errorCode != MegaError.API_OK
            ) {
                return false
            }
        }

        return true
    }

    /**
     * Check if all nodes are file nodes and not taken down.
     *
     * @param nodes nodes to check
     * @return whether all nodes are file nodes and not taken down.
     */
    @JvmStatic
    fun areAllFileNodesAndNotTakenDown(nodes: List<MegaNode>): Boolean {
        for (node in nodes) {
            if (!node.isFile || node.isTakenDown) {
                return false
            }
        }

        return true
    }

    /**
     * Check if all nodes are not taken down.
     *
     * @return True if all items are not taken down, false otherwise.
     */
    @JvmStatic
    fun List<MegaNode?>.areAllNotTakenDown(): Boolean {
        for (node in this) {
            if (node?.isTakenDown == true) {
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
            if (megaApi.checkAccessErrorExtended(
                    node,
                    MegaShare.ACCESS_FULL
                ).errorCode != MegaError.API_OK
            ) {
                return false
            }
        }

        return true
    }

    /**
     * Check if all nodes have owner access and are not taken down.
     *
     * @param nodes List of nodes to check.
     * @return True if all nodes have owner access and are not taken down, false otherwise.
     */
    @JvmStatic
    fun allHaveOwnerAccessAndNotTakenDown(nodes: List<MegaNode?>): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi

        for (node in nodes) {
            if (megaApi.checkAccessErrorExtended(
                    node,
                    MegaShare.ACCESS_OWNER
                ).errorCode != MegaError.API_OK
                || node?.isTakenDown == true
            ) {
                return false
            }
        }

        return true
    }

    /**
     * Shows a confirmation warning before leave an incoming share.
     *
     * @param activity current Activity
     * @param snackbarShower interface to show snackbar
     * @param node incoming share to leave
     */
    @JvmStatic
    fun showConfirmationLeaveIncomingShare(
        activity: Activity,
        snackbarShower: SnackbarShower,
        node: MegaNode,
    ) {
        showConfirmationLeaveIncomingShares(activity, snackbarShower, node, null)
    }

    /**
     * Shows a confirmation warning before leave some incoming shares.
     *
     * @param activity current Activity
     * @param snackbarShower interface to show snackbar
     * @param handleList    handles list of the incoming shares to leave
     */
    @JvmStatic
    fun showConfirmationLeaveIncomingShares(
        activity: Activity,
        snackbarShower: SnackbarShower,
        handleList: ArrayList<Long>,
    ) {
        showConfirmationLeaveIncomingShares(activity, snackbarShower, null, handleList)
    }

    /**
     * Shows a confirmation warning before leave one or more incoming shares.
     *
     * @param activity current Activity
     * @param snackbarShower interface to show snackbar
     * @param node if only one incoming share to leave, its node, null otherwise
     * @param handles if mode than one incoming shares to leave, list of its handles, null otherwise
     */
    private fun showConfirmationLeaveIncomingShares(
        activity: Activity,
        snackbarShower: SnackbarShower,
        node: MegaNode?,
        handles: ArrayList<Long>?,
    ) {
        val onlyOneIncomingShare = node != null && handles == null
        val numIncomingShares = if (onlyOneIncomingShare) 1 else handles!!.size
        val builder = MaterialAlertDialogBuilder(activity)

        builder.setMessage(
            activity.resources.getQuantityString(
                R.plurals.confirmation_leave_share_folder,
                numIncomingShares
            )
        )
            .setPositiveButton(activity.getString(R.string.general_leave)) { _, _ ->
                if (onlyOneIncomingShare) {
                    leaveIncomingShare(
                        snackbarShower = snackbarShower,
                        node = node!!,
                        context = activity
                    )
                } else {
                    leaveMultipleIncomingShares(activity, snackbarShower, handles!!)
                }
                MegaApplication.getInstance()
                    .sendBroadcast(Intent(BroadcastConstants.BROADCAST_ACTION_DESTROY_ACTION_MODE))
            }
            .setNegativeButton(activity.getString(R.string.general_cancel), null)
            .show()
    }

    /**
     * Leave incoming share.
     *
     * @param snackbarShower interface to show snackbar
     * @param node node to leave incoming share
     */
    private fun leaveIncomingShare(
        snackbarShower: SnackbarShower,
        node: MegaNode?,
        context: Context,
    ) {
        Timber.d("Node handle: ${node?.handle}")
        MegaApplication.getInstance().megaApi.remove(
            node,
            RemoveListener(
                snackbarShower = snackbarShower,
                isIncomingShare = true,
                context = context
            )
        )
    }

    /**
     * Leave multiple incoming shares.
     *
     * @param activity current Activity
     * @param snackbarShower interface to show snackbar
     * @param handles handles of nodes to leave incoming share
     */
    private fun leaveMultipleIncomingShares(
        activity: Activity,
        snackbarShower: SnackbarShower,
        handles: List<Long>,
    ) {
        Timber.d("Leaving ${handles.size} incoming shares")

        val megaApi = MegaApplication.getInstance().megaApi

        if (handles.size == 1) {
            leaveIncomingShare(
                snackbarShower = snackbarShower,
                node = megaApi.getNodeByHandle(handles[0]),
                context = activity
            )
            return
        }

        val moveMultipleListener = MultipleRequestListener(MULTIPLE_LEAVE_SHARE, activity)
        for (handle in handles) {
            val node = megaApi.getNodeByHandle(handle)
            megaApi.remove(node, moveMultipleListener)
        }
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
        parent: MegaNode?, folder: File,
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
    fun getNodeLabelText(nodeLabel: Int, context: Context): String = context.getString(
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

            return rootNode?.handle ?: INVALID_HANDLE
        }

    /**
     * Setup SDK HTTP streaming server.
     *
     * @param api MegaApiAndroid instance to use
     * @param context Android context
     * @return whether this function call really starts SDK HTTP streaming server
     */
    @JvmStatic
    fun setupStreamingServer(api: MegaApiAndroid, context: Context): Boolean {
        if (api.httpServerIsRunning() == 0) {
            api.httpServerStart()

            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)

            api.httpServerSetMaxBufferSize(
                if (memoryInfo.totalMem > BUFFER_COMP) MAX_BUFFER_32MB
                else MAX_BUFFER_16MB
            )

            return true
        }

        return false
    }

    /**
     * Stop SDK HTTP streaming server.
     *
     * @param shouldStopServer True if should stop the server, false otherwise.
     * @param megaApi          MegaApiAndroid instance to use.
     */
    @JvmStatic
    fun stopStreamingServerIfNeeded(shouldStopServer: Boolean, megaApi: MegaApiAndroid) {
        if (shouldStopServer) {
            megaApi.httpServerStop()
        }
    }

    /**
     * show dialog
     *
     * @param isFolder        the clicked node
     * @param listener        the listener to handle all clicking event
     * @param context         the context where adapter resides
     * @return the dialog object to be handled by adapter to be dismissed, in case of window leaking situation
     */
    @JvmStatic
    fun showTakenDownDialog(
        isFolder: Boolean,
        listener: NodeTakenDownDialogListener? = null,
        context: Context,
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context)

        builder.setTitle(
            if (isFolder) context.getString(R.string.dialog_taken_down_folder_title) else context.getString(
                R.string.dialog_taken_down_file_title
            )
        )
        val text =
            if (isFolder) context.getString(R.string.dialog_taken_down_folder_description) else context.getString(
                R.string.dialog_taken_down_file_description
            )
        val startIndex = text.indexOf("[A]")
        var formatterText = text.replace("[A]", "")
        val endIndex = formatterText.indexOf("[/A]")
        formatterText = formatterText.replace("[/A]", "")
        val urlSpan = SpannableString(formatterText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val openTermsIntent = Intent(context, WebViewActivity::class.java)
                openTermsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                openTermsIntent.data = Uri.parse(Constants.TAKEDOWN_URL)
                context.startActivity(openTermsIntent)
            }
        }
        urlSpan.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.setMessage(urlSpan)
        builder.setPositiveButton(context.getString(R.string.general_ok)) { dialog, _ ->
            dialog.dismiss()
            listener?.onCancelClicked()
        }
        builder.setNegativeButton(context.getString(R.string.dispute_takendown_file)) { dialog, _ ->
            val openTermsIntent = Intent(context, WebViewActivity::class.java)
            openTermsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            openTermsIntent.data = Uri.parse(Constants.DISPUTE_URL)
            context.startActivity(openTermsIntent)
            dialog.dismiss()
            listener?.onDisputeClicked()
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()
        val messageText = dialog.findViewById<TextView>(android.R.id.message)
        messageText?.let {
            it.movementMethod = LinkMovementMethod.getInstance()
        }
        return dialog
    }

    /**
     * Start [FileExplorerActivity] to select folder to move nodes.
     *
     * @param activity current Android activity
     * @param handles handles to move
     */
    @JvmStatic
    fun selectFolderToMove(activity: Activity, handles: LongArray) {
        val intent = Intent(activity, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
        intent.putExtra(INTENT_EXTRA_KEY_MOVE_FROM, handles)
        activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_MOVE)
    }

    /**
     * Start [FileExplorerActivity] to select folder to copy nodes.
     *
     * @param activity current Android activity
     * @param handles handles to copy
     */
    @JvmStatic
    fun selectFolderToCopy(activity: Activity, handles: LongArray) {
        if (getStorageState() == StorageState.PayWall) {
            AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
            return
        }

        val intent = Intent(activity, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_COPY_FOLDER
        intent.putExtra(INTENT_EXTRA_KEY_COPY_FROM, handles)
        activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_COPY)
    }

    /**
     * Get location info of a node.
     *
     * @param adapterType node source adapter type
     * @param fromIncomingShare is from incoming share
     * @param handle node handle
     *
     * @return location info
     */
    @JvmStatic
    fun getNodeLocationInfo(
        adapterType: Int,
        fromIncomingShare: Boolean,
        handle: Long,
    ): LocationInfo? {
        val app = MegaApplication.getInstance()
        val dbHandler = getDbHandler()
        val megaApi = app.megaApi

        if (adapterType == OFFLINE_ADAPTER) {
            val node = dbHandler.findByHandle(handle) ?: return null
            val file = OfflineUtils.getOfflineFile(app, node)
            if (!file.exists()) {
                return null
            }

            val parentName = file.parentFile?.name ?: return null
            val grandParentName = file.parentFile?.parentFile?.name
            val location = when {
                grandParentName != null
                        && grandParentName + File.separator + parentName == OfflineUtils.OFFLINE_INBOX_DIR -> {
                    app.getString(R.string.section_saved_for_offline_new)
                }

                parentName == OfflineUtils.OFFLINE_DIR -> {
                    app.getString(R.string.section_saved_for_offline_new)
                }

                else -> {
                    app.getString(
                        R.string.location_label, parentName,
                        app.getString(R.string.section_saved_for_offline_new)
                    )
                }
            }

            return LocationInfo(location, offlineParentPath = node.path)
        } else {
            val node = megaApi.getNodeByHandle(handle) ?: return null

            val parent = megaApi.getParentNode(node)
            val topAncestor = megaApi.getRootParentNode(node)

            val inCloudDrive = topAncestor.handle == megaApi.rootNode?.handle
                    || topAncestor.handle == megaApi.rubbishNode?.handle
            val inBackups = topAncestor.handle == megaApi.inboxNode?.handle

            val location = when {
                fromIncomingShare -> {
                    if (parent != null) {
                        app.getString(
                            R.string.location_label, parent.name,
                            app.getString(R.string.tab_incoming_shares)
                        )
                    } else {
                        app.getString(R.string.tab_incoming_shares)
                    }
                }

                parent == null -> {
                    app.getString(R.string.tab_incoming_shares)
                }

                inCloudDrive -> {
                    if (topAncestor.handle == parent.handle) {
                        getTranslatedNameForParentNode(megaApi, topAncestor, app)
                    } else {
                        app.getString(
                            R.string.location_label, parent.name,
                            getTranslatedNameForParentNode(megaApi, topAncestor, app)
                        )
                    }
                }

                inBackups -> {
                    if (node.parentHandle == myBackupHandle) {
                        // If the Node's parent handle is the same with the My Backups handle,
                        // only display the name of the Root Node
                        getTranslatedNameForParentNode(megaApi, topAncestor, app)
                    } else {
                        // Otherwise, include the names of both the Parent and Root Nodes
                        app.getString(
                            R.string.location_label, parent.name,
                            getTranslatedNameForParentNode(megaApi, topAncestor, app)
                        )
                    }
                }

                else -> {
                    app.getString(
                        R.string.location_label, parent.name,
                        app.getString(R.string.tab_incoming_shares)
                    )
                }
            }

            val fragmentHandle = when {
                fromIncomingShare || parent == null -> INVALID_HANDLE
                inCloudDrive -> topAncestor.handle
                else -> INVALID_HANDLE
            }

            return LocationInfo(
                location,
                parentHandle = parent?.handle ?: INVALID_HANDLE,
                fragmentHandle = fragmentHandle
            )
        }
    }

    /**
     * Handle click event of the location text.
     *
     * @param activity current activity
     * @param adapterType node source adapter type
     * @param location location info
     */
    @JvmStatic
    fun handleLocationClick(activity: Activity, adapterType: Int, location: LocationInfo) {
        val intent = Intent(activity, ManagerActivity::class.java)

        intent.action = ACTION_OPEN_FOLDER
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra(INTENT_EXTRA_KEY_LOCATION_FILE_INFO, true)

        if (adapterType == OFFLINE_ADAPTER) {
            intent.putExtra(INTENT_EXTRA_KEY_OFFLINE_ADAPTER, true)

            if (location.offlineParentPath != null) {
                intent.putExtra(INTENT_EXTRA_KEY_PATH_NAVIGATION, location.offlineParentPath)
            }
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_FRAGMENT_HANDLE, location.fragmentHandle)

            if (location.parentHandle != INVALID_HANDLE) {
                intent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, location.parentHandle)
            }
        }

        activity.startActivity(intent)
        activity.finish()
    }

    private fun getTranslatedNameForParentNode(
        megaApi: MegaApiAndroid,
        parent: MegaNode,
        context: Context,
    ): String {
        return when {
            parent.handle == megaApi.rootNode?.handle -> context.getString(R.string.section_cloud_drive)
            parent.handle == megaApi.rubbishNode?.handle -> context.getString(R.string.section_rubbish_bin)
            parent.handle == megaApi.inboxNode?.handle -> context.getString(R.string.home_side_menu_backups_title)
            else -> parent.name
        }
    }

    /**
     * Auto play a node when it's downloaded.
     *
     * @param context Android context
     * @param autoPlayInfo auto play info
     * @param activityLauncher interface to launch activity
     * @param snackbarShower interface to show snackbar
     */
    @JvmStatic
    fun autoPlayNode(
        context: Context,
        autoPlayInfo: AutoPlayInfo,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
    ) {
        val mime = MimeTypeList.typeForName(autoPlayInfo.nodeName)
        when {
            // // ZIP file on SD card can't not be created by `new java.util.zip.ZipFile(path)`.
            mime.isZip && !SDCardUtils.isLocalFolderOnSDCard(context, autoPlayInfo.localPath) -> {
                openZip(
                    context = context,
                    activityLauncher = activityLauncher,
                    zipFilePath = autoPlayInfo.localPath,
                    snackbarShower = snackbarShower,
                    nodeHandle = autoPlayInfo.nodeHandle
                )
            }

            mime.isPdf -> {
                val pdfIntent = Intent(context, PdfViewerActivity::class.java)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, autoPlayInfo.nodeHandle)

                if (!setLocalIntentParams(
                        context, autoPlayInfo.nodeName, pdfIntent, autoPlayInfo.localPath,
                        false, snackbarShower
                    )
                ) {
                    return
                }

                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_INSIDE, true)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_IS_URL, false)

                activityLauncher.launchActivity(pdfIntent)
            }

            mime.isVideoMimeType || mime.isAudio -> {
                val mediaIntent: Intent
                val internalIntent: Boolean
                var opusFile = false
                if (mime.isVideoNotSupported || mime.isAudioNotSupported
                ) {
                    mediaIntent = Intent(Intent.ACTION_VIEW)
                    internalIntent = false
                    val parts = autoPlayInfo.nodeName.split("\\.")
                    if (parts.size > 1 && parts.last() == "opus") {
                        opusFile = true
                    }
                } else {
                    internalIntent = true
                    mediaIntent = getMediaIntent(context, autoPlayInfo.nodeName)
                }
                mediaIntent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, autoPlayInfo.nodeHandle)

                if (!setLocalIntentParams(
                        context, autoPlayInfo.nodeName, mediaIntent, autoPlayInfo.localPath,
                        false, snackbarShower
                    )
                ) {
                    return
                }

                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                mediaIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                if (opusFile) {
                    mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
                }

                if (internalIntent) {
                    activityLauncher.launchActivity(mediaIntent)
                } else {
                    if (isIntentAvailable(context, mediaIntent)) {
                        activityLauncher.launchActivity(mediaIntent)
                    } else {
                        sendFile(
                            context,
                            autoPlayInfo.nodeName,
                            autoPlayInfo.localPath,
                            activityLauncher,
                            snackbarShower
                        )
                    }
                }
            }

            else -> {
                launchActionView(
                    context,
                    autoPlayInfo.nodeName,
                    autoPlayInfo.localPath,
                    activityLauncher,
                    snackbarShower
                )
            }
        }
    }

    /**
     * Launch [ZipBrowserActivity] to preview a zip file.
     *
     * @param context Android context.
     * @param activityLauncher interface to launch activity.
     * @param zipFilePath The local path of the zip file.
     * @param snackbarShower interface to snackbar shower
     * @param nodeHandle The handle of the corresponding node.
     */
    @JvmStatic
    fun openZip(
        context: Context,
        activityLauncher: ActivityLauncher,
        zipFilePath: String,
        snackbarShower: SnackbarShower,
        nodeHandle: Long,
    ) {
        if (ZipBrowserActivity.zipFileFormatCheck(context, zipFilePath)) {
            activityLauncher.launchActivity(Intent(context, ZipBrowserActivity::class.java).apply {
                putExtra(
                    ZipBrowserActivity.EXTRA_PATH_ZIP, zipFilePath
                )
                putExtra(
                    ZipBrowserActivity.EXTRA_HANDLE_ZIP, nodeHandle
                )
            })
        } else {
            snackbarShower.showSnackbar(context.getString(R.string.message_zip_format_error))
        }
    }

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
    @JvmStatic
    fun launchActionView(
        context: Context,
        nodeName: String,
        localPath: String,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
    ) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW)

            if (!setLocalIntentParams(
                    context, nodeName, viewIntent,
                    localPath, false, snackbarShower
                )
            ) {
                return
            }

            viewIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (isIntentAvailable(context, viewIntent)) {
                activityLauncher.launchActivity(viewIntent)
            } else {
                sendFile(
                    context,
                    nodeName,
                    localPath,
                    activityLauncher,
                    snackbarShower
                )
            }
        } catch (e: Exception) {
            snackbarShower.showSnackbar(context.getString(R.string.general_already_downloaded))
        }
    }

    /**
     * Create an Intent with ACTION_SEND for an auto play file.
     *
     * @param context Android context
     * @param nodeName Name of the node.
     * @param localPath Local path of the node.
     * @param activityLauncher interface to launch activity
     * @param snackbarShower interface to show snackbar
     */
    private fun sendFile(
        context: Context,
        nodeName: String,
        localPath: String,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
    ) {
        val intentShare = Intent(Intent.ACTION_SEND)

        if (!setLocalIntentParams(
                context, nodeName, intentShare,
                localPath, false, snackbarShower
            )
        ) {
            return
        }

        intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (isIntentAvailable(context, intentShare)) {
            activityLauncher.launchActivity(intentShare)
        } else {
            snackbarShower.showSnackbar(context.getString(R.string.intent_not_available))
        }
    }

    /**
     * Gets the string to show as file info details with the next format: "size · modification date".
     *
     * @param node The file node from which to get the details.
     * @return The string so show as file info details.
     */
    @JvmStatic
    fun getFileInfo(node: MegaNode, context: Context): String? {
        return TextUtil.getFileInfo(
            getSizeString(node.size, context),
            formatLongDateTime(node.modificationTime)
        )
    }

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context     Current context.
     * @param node        Node to preview on Text Editor.
     * @param adapterType Current adapter view.
     */
    @JvmStatic
    fun manageTextFileIntent(context: Context, node: MegaNode, adapterType: Int) {
        manageTextFileIntent(context, node, adapterType, null, VIEW_MODE)
    }

    /**
     * Launches an Intent to open TextFileEditorActivity on edit mode.
     *
     * @param context     Current context.
     * @param node        Node to preview on Text Editor.
     * @param adapterType Current adapter view.
     */
    @JvmStatic
    fun manageEditTextFileIntent(context: Context, node: MegaNode, adapterType: Int) {
        manageTextFileIntent(context, node, adapterType, null, EDIT_MODE)
    }

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context     Current context.
     * @param node        Node to preview on Text Editor.
     * @param adapterType Current adapter view.
     * @param urlFileLink Link of the file if the adapter is FILE_LINK_ADAPTER.
     */
    @JvmStatic
    fun manageTextFileIntent(
        context: Context,
        node: MegaNode,
        adapterType: Int,
        urlFileLink: String?,
    ) {
        manageTextFileIntent(context, node, adapterType, urlFileLink, VIEW_MODE)
    }

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context     Current context.
     * @param node        Node to preview on Text Editor.
     * @param adapterType Current adapter view.
     * @param urlFileLink Link of the file if the adapter is FILE_LINK_ADAPTER.
     * @param mode        Text file editor mode.
     */
    @JvmStatic
    fun manageTextFileIntent(
        context: Context,
        node: MegaNode,
        adapterType: Int,
        urlFileLink: String?,
        mode: String,
    ) {
        val textFileIntent = Intent(context, TextEditorActivity::class.java)

        if (adapterType == FILE_LINK_ADAPTER) {
            textFileIntent.putExtra(EXTRA_SERIALIZE_STRING, node.serialize())
                .putExtra(URL_FILE_LINK, urlFileLink)
        } else {
            textFileIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.handle)
        }

        textFileIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, adapterType)
            .putExtra(MODE, mode)
        context.startActivity(textFileIntent)
    }

    /**
     * Opens an URL node.
     *
     * @param context Current context.
     * @param megaApi MegaApiAndroid instance to use.
     * @param node    MegaNode which contains an URL to open.
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun manageURLNode(context: Context, megaApi: MegaApiAndroid, node: MegaNode): Disposable {
        val progressDialog = android.app.ProgressDialog(context)
        progressDialog.apply {
            setMessage(context.getString(R.string.link_request_status))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }

        return Completable.create { emitter ->
            val localPath = getLocalFile(node)
            var br: BufferedReader? = null
            var shouldStopServer = false

            if (localPath != null) {
                //Read local file
                val localFile = File(localPath)
                br = BufferedReader(FileReader(localFile))
            } else {
                //Read streaming file
                shouldStopServer = setupStreamingServer(megaApi, context)
                val uri = megaApi.httpServerGetLocalLink(node)

                if (!uri.isNullOrEmpty()) {
                    val nodeURL = URL(uri)
                    val connection = nodeURL.openConnection() as HttpURLConnection
                    br = BufferedReader(InputStreamReader(connection.inputStream))
                }
            }

            if (br != null) {
                var line = br.readLine()

                if (line != null) {
                    line = br.readLine()
                    val url = line.replace(URL_INDICATOR, "")
                    val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))

                    if (isIntentAvailable(context, intent)) {
                        context.startActivity(intent)
                    } else {
                        showSnackbar(context, context.getString(R.string.intent_not_available_file))
                    }

                    stopStreamingServerIfNeeded(shouldStopServer, megaApi)
                    emitter.onComplete()
                    return@create
                } else {
                    Timber.d("Not expected format: Exception on processing url file")
                }
            }

            emitter.onError(Throwable("Error getting URL"))
            stopStreamingServerIfNeeded(shouldStopServer, megaApi)
            showSnackbar(context, context.getString(R.string.error_open_file_with))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { progressDialog.dismiss() },
                onError = { error ->
                    progressDialog.dismiss()
                    Timber.e(error.message)
                }
            )
    }

    /**
     * Handle the event when a node is tapped.
     *
     * @param context Android context
     * @param node The node tapped.
     * @param nodeDownloader Function/Methd for downloading node.
     * @param activityLauncher interface to launch activity
     * @param snackbarShower interface to show snackbar
     */
    @JvmStatic
    fun onNodeTapped(
        context: Context,
        node: MegaNode,
        nodeDownloader: (node: MegaNode) -> Unit,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
        isOpenWith: Boolean = false,
    ) {
        val possibleLocalFile = getTappedNodeLocalFile(node)

        if (possibleLocalFile != null) {
            Timber.d("The node is already downloaded, found in local.")

            // ZIP file on SD card can't not be created by `new java.util.zip.ZipFile(path)`.
            if (MimeTypeList.typeForName(node.name).isZip && !SDCardUtils.isLocalFolderOnSDCard(
                    context,
                    possibleLocalFile
                ) && !isOpenWith
            ) {
                Timber.d("The file is zip, open in-app.")
                openZip(
                    context = context,
                    activityLauncher = activityLauncher,
                    zipFilePath = possibleLocalFile,
                    snackbarShower = snackbarShower,
                    nodeHandle = node.handle
                )
            } else {
                Timber.d("The file cannot be opened in-app.")
                launchActionView(
                    context,
                    node.name,
                    possibleLocalFile,
                    activityLauncher,
                    snackbarShower
                )
            }
        } else {
            nodeDownloader(node)
        }
    }

    /**
     * Check the folder of My Backup and get the folder node
     *
     * @param megaApi MegaApiAndroid instance to use.
     * @param handleList handles list of the nodes that selected
     * @return The node of My Backups or null
     */
    @JvmStatic
    fun getBackupRootNodeByHandle(
        megaApi: MegaApiAndroid,
        handleList: ArrayList<Long>?,
    ): MegaNode? {
        if (handleList != null && handleList.size > 0) {
            for (handle in handleList) {
                val p: MegaNode? = megaApi.getNodeByHandle(handle)
                if (p?.handle == myBackupHandle) {
                    return p
                }
            }
        }
        return null
    }

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
    @JvmStatic
    fun checkBackupNodeTypeInList(megaApi: MegaApiAndroid, handleList: List<Long>?): Int {
        if (handleList == null) return BACKUP_NONE

        if (handleList.size == 1) {
            // Only one node
            val p: MegaNode? = megaApi.getNodeByHandle(handleList[0])
            return checkBackupNodeTypeByHandle(megaApi, p)
        } else {
            val node = megaApi.getNodeByHandle(handleList[0])

            when (node?.parentHandle) {
                // the nodes in the handleList belong to the root node
                megaApi.rootNode?.handle -> {
                    // Check if handleList contains backup root node
                    return handleList.firstOrNull {
                        isRootBackupFolder(megaApi.getNodeByHandle(it))
                    }?.let { BACKUP_ROOT } ?: BACKUP_NONE
                }

                // the nodes in the handleList belong to "My Backups"
                myBackupHandle -> {
                    // Check if handleList contains device nodes
                    return handleList.firstOrNull {
                        isDeviceBackupFolder(megaApi.getNodeByHandle(it))
                    }?.let { BACKUP_DEVICE } ?: BACKUP_NONE
                }

                else -> {
                    return handleList.firstNotNullOfOrNull {
                        val nodeType =
                            checkBackupNodeTypeByHandle(megaApi, megaApi.getNodeByHandle(it))
                        if (nodeType != BACKUP_NONE) nodeType else null
                    } ?: BACKUP_NONE
                }
            }
        }
    }

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
    @JvmStatic
    fun checkBackupNodeTypeByHandle(megaApi: MegaApiAndroid, node: MegaNode?): Int =
        node?.let { selectedNode ->
            Timber.d("MyBackup + node.handle = ${node.handle}")

            // First, check if the node exists in Backups.
            // If the node doesn't exist in Backups, or is in Rubbish Bin, return BACKUP_NONE
            if (!megaApi.isInInbox(selectedNode) || isNodeInRubbishOrDeleted(selectedNode.handle)) {
                Timber.d("MyBackup + checkBackupNodeTypeByHandle return nodeType = $BACKUP_NONE")
                return BACKUP_NONE
            }

            // If the node exists in Backups, check and return BACKUP_ROOT
            // if the node's handle is the same with the root Backup handle
            if (selectedNode.handle == myBackupHandle) {
                Timber.d("MyBackup + checkBackupNodeTypeByHandle return nodeType = $BACKUP_ROOT")
                return BACKUP_ROOT
            }

            // If the node is not a Backup Root node, check and return BACKUP_DEVICE
            // if the node's parent handle is the same with the root Backup handle, and
            // if the node's device ID exists
            if (selectedNode.parentHandle == myBackupHandle && !selectedNode.deviceId.isNullOrBlank()) {
                Timber.d("MyBackup + checkBackupNodeTypeByHandle return nodeType = $BACKUP_DEVICE")
                return BACKUP_DEVICE
            }

            // If the node is not a Device node, check and return BACKUP_FOLDER
            // if the node's parent node is a Device Node, and if the Device Node's ID exists
            val deviceNode = megaApi.getNodeByHandle(selectedNode.parentHandle)
            if ((selectedNode.parentHandle == deviceNode?.handle) && !deviceNode.deviceId.isNullOrBlank()) {
                Timber.d("MyBackup + checkBackupNodeTypeByHandle return nodeType = $BACKUP_FOLDER")
                return BACKUP_FOLDER
            }

            // Otherwise, the node is considered to be a Child Folder of a Backup Folder underneath
            // the Device Node
            Timber.d("MyBackup + checkBackupNodeTypeByHandle return nodeType = $BACKUP_FOLDER_CHILD")
            return BACKUP_FOLDER_CHILD

        } ?: BACKUP_NONE

    @JvmStatic
    fun containsMediaFile(handle: Long): Boolean {
        val megaApi = MegaApplication.getInstance().megaApi
        val parent = megaApi.getNodeByHandle(handle)
        val children: List<MegaNode?>? = megaApi.getChildren(parent)

        children?.forEach {
            val mime = MimeTypeList.typeForName(it?.name)
            if (!mime.isSvgMimeType && (mime.isImage || mime.isVideoMimeType)) return true
        }

        return false
    }

    @JvmStatic
    fun containsMediaFile(handles: List<MegaNode>): Boolean {
        handles.forEach {
            val mime = MimeTypeList.typeForName(it?.name)
            if (!mime.isSvgMimeType && (mime.isImage || mime.isVideoMimeType)) return true
        }

        return false
    }

    fun MegaNode.getFileName(): String =
        "$base64Handle.${MimeTypeList.typeForName(name)?.extension}"

    fun MegaNode.getThumbnailFileName(): String =
        "$base64Handle${JPG_EXTENSION}"

    fun MegaNode.getPreviewFileName(): String =
        "$base64Handle${JPG_EXTENSION}"

    fun MegaNode.isImage(): Boolean =
        this.isFile && MimeTypeList.typeForName(name).isImage

    fun MegaNode.isGif(): Boolean =
        this.isFile && MimeTypeList.typeForName(name).isGIF

    fun MegaNode.isVideo(): Boolean =
        this.isFile && (MimeTypeList.typeForName(name).isVideoMimeType ||
                MimeTypeList.typeForName(name).isMp4Video)

    fun MegaNode.getLastAvailableTime(): Long =
        when {
            creationTime > 1 -> creationTime
            modificationTime > 1 -> modificationTime
            publicLinkCreationTime > 1 -> publicLinkCreationTime
            else -> INVALID_VALUE.toLong()
        }

    /**
     * Check if a specific MegaNode is valid for Image Viewer
     *
     * @return  True if it's valid, false otherwise
     */
    @JvmStatic
    fun MegaNode.isValidForImageViewer(): Boolean =
        isFile && (isImage() || isGif() || isVideo())

    /**
     * Check if a specific MegaOffline is valid for Image Viewer
     *
     * @return  True if it's valid, false otherwise
     */
    @JvmStatic
    fun MegaOffline.isValidForImageViewer(): Boolean =
        !isFolder && (MimeTypeList.typeForName(name).isImage
                || MimeTypeList.typeForName(name).isGIF
                || (MimeTypeList.typeForName(name).isVideoMimeType || MimeTypeList.typeForName(
            name
        ).isMp4Video))

    /**
     * Check if provided node File is valid for the specified MegaNode
     *
     * @param node      MegaNode to be compared with
     * @param nodeFile  Node file to be compared with
     * @return          true if its valid, false otherwise
     */
    @JvmStatic
    fun MegaApiAndroid.checkValidNodeFile(node: MegaNode, nodeFile: File?): Boolean =
        nodeFile?.canRead() == true && nodeFile.length() == node.size
                && node.fingerprint == getFingerprint(nodeFile.absolutePath)

    /**
     * Generate MegaNode information preformatted text
     *
     * @return MegaNode information
     */
    fun MegaNode.getInfoText(context: Context): String {
        val nodeSizeText = getSizeString(size, context)
        val nodeDateText = formatLongDateTime(getLastAvailableTime())
        return TextUtil.getFileInfo(nodeSizeText, nodeDateText)
    }

    /**
     * Generate MegaOffline information preformatted text
     *
     * @return MegaOffline information
     */
    fun MegaOffline.getInfoText(context: Context): String {
        val nodeSizeText = getSizeString(getSize(context), context)
        val nodeDateText = formatLongDateTime(getModificationDate(context))
        return TextUtil.getFileInfo(nodeSizeText, nodeDateText)
    }
}
