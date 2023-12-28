package mega.privacy.android.app.sync.fileBackups

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.interfaces.ActionBackupNodeCallback
import mega.privacy.android.app.main.FileContactListActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_NONE
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_WARNING
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_CANCEL
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_MENU_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.createBackupsWarningDialog
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeInList
import mega.privacy.android.app.utils.MegaNodeUtil.getBackupRootNodeByHandle
import nz.mega.sdk.MegaNode
import timber.log.Timber

/**
 * Manager class used to process actions related to Backup nodes.
 */
class FileBackupManager(
    val activity: Activity,
    val actionBackupListener: ActionBackupListener?,
) {

    object BackupDialogState {
        const val BACKUP_DIALOG_SHOW_NONE = -1
        const val BACKUP_DIALOG_SHOW_WARNING = 0
    }

    object OperationType {
        const val OPERATION_NONE = -1
        const val OPERATION_CANCEL = 0
        const val OPERATION_EXECUTE = 2
    }

    private val megaApplication = MegaApplication.getInstance()
    private val megaApi = megaApplication.megaApi
    private var nodeController: NodeController? = null

    var backupWarningDialog: AlertDialog? = null
    var backupHandleList: ArrayList<Long>? = null
    var backupDialogType: Int = BACKUP_DIALOG_SHOW_NONE
    var backupNodeHandle: Long? = null
    var backupNodeType = 0
    var backupActionType = 0

    init {
        initBackupWarningState()
    }

    /**
     * Initialize the fields of FileBackupManager
     */
    private fun initBackupWarningState() {
        backupHandleList = null
        backupNodeHandle = -1L
        backupNodeType = -1
        backupActionType = -1
        backupDialogType = BACKUP_DIALOG_SHOW_NONE
    }

    /**
     * An [ActionBackupNodeCallback] implementation that is used if there are
     * custom actions to be implemented in the Override methods
     */
    val defaultActionBackupNodeCallback = object : ActionBackupNodeCallback {
        override fun actionCancel(dialog: DialogInterface?, actionType: Int) {
            initBackupWarningState()
            actionBackupListener?.actionBackupResult(actionType, OPERATION_CANCEL)
        }

        override fun actionExecute(
            handleList: ArrayList<Long>?,
            megaNode: MegaNode?,
            nodeType: Int,
            actionType: Int,
        ) {
            initBackupWarningState()
            actionBackupListener?.actionBackupResult(actionType, OPERATION_EXECUTE)
        }
    }

    /**
     * An [ActionBackupNodeCallback] implementation that specifies exact actions
     */
    val actionBackupNodeCallback = object : ActionBackupNodeCallback {
        override fun actionCancel(dialog: DialogInterface?, actionType: Int) {
            initBackupWarningState()
            actionBackupListener?.actionBackupResult(actionType, OPERATION_CANCEL)
        }

        override fun actionExecute(
            handleList: ArrayList<Long>?,
            megaNode: MegaNode?,
            nodeType: Int,
            actionType: Int,
        ) {
            initBackupWarningState()
            when (actionType) {
                ACTION_BACKUP_SHARE_FOLDER -> if (MegaNodeUtil.isOutShare(megaNode)) {
                    val i = Intent(
                        activity,
                        FileContactListActivity::class.java
                    )
                    i.putExtra(Constants.NAME, megaNode?.handle)
                    activity.startActivity(i)
                } else {
                    nodeController?.selectContactToShareFolder(megaNode)
                }

                ACTION_MENU_BACKUP_SHARE_FOLDER -> nodeController?.selectContactToShareFolders(
                    handleList
                )
            }
        }
    }

    /**
     * Share the nodes that contain the backup folder
     * or folders beneath the backup folder
     *
     * @param nC The instance of NodeController
     * @param nodeHandles The nodes to share
     * @param contactsData The contacts that to share
     * @param accessType Permissions that are granted to the user
     * @return true if the backup nodes in [nodeHandles], false if no backup node in [nodeHandles]
     */
    fun shareFolder(
        nC: NodeController,
        nodeHandles: LongArray,
        contactsData: ArrayList<String>,
        accessType: Int,
    ): Boolean {
        val handleList = ArrayList<Long>()
        nodeHandles.toCollection(handleList)
        val pNode = getBackupRootNodeByHandle(megaApi, handleList)
        val nodeType = checkBackupNodeTypeInList(megaApi, handleList)

        if (nodeType != BACKUP_NONE || pNode != null) {
            Timber.d("shareFolder with accessType = $accessType")
            nC.shareFolders(nodeHandles, contactsData, accessType)
            return true
        }
        return false
    }

    /**
     * Displays a Warning Dialog when attempting to share a Backups folder
     *
     * @param handleList The list of Node Handles
     * @param megaNode The Backup Node
     * @param nodeType The Backup Node Type - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file - ACTION_MENU_BACKUP_SHARE_FOLDER / ACTION_BACKUP_SHARE_FOLDER
     * @param actionBackupNodeCallback The callback for Backup Node Actions
     */
    fun showBackupsWarningDialog(
        handleList: ArrayList<Long>?,
        megaNode: MegaNode?,
        nodeType: Int,
        actionType: Int,
        actionBackupNodeCallback: ActionBackupNodeCallback,
    ) {
        backupHandleList = handleList
        backupNodeHandle = megaNode?.handle
        backupNodeType = nodeType
        backupActionType = actionType
        backupDialogType = BACKUP_DIALOG_SHOW_WARNING

        backupWarningDialog = createBackupsWarningDialog(
            activity = activity,
            actionBackupNodeCallback = actionBackupNodeCallback,
            handleList = handleList,
            megaNode = megaNode,
            nodeType = nodeType,
            actionType = actionType,
        )
    }

    /**
     * Checks the list of Node Handles if there are Backup Nodes or not
     *
     * @param nodeController The [NodeController]
     * @param handleList The list of Node Handles
     * @param actionBackupNodeCallback The callback for Backup Node actions
     *
     * @return true if there is at least one Backup Node from the list of Node Handles, and false
     * if otherwise
     */
    fun hasBackupsNodes(
        nodeController: NodeController,
        handleList: ArrayList<Long>,
        actionBackupNodeCallback: ActionBackupNodeCallback,
    ): Boolean {
        val backupRootNode = getBackupRootNodeByHandle(megaApi, handleList)
        val backupNodeType = checkBackupNodeTypeInList(megaApi, handleList)

        if (backupNodeType == BACKUP_NONE && backupRootNode == null) {
            // No backup node in the selected nodes
            return false
        } else if (backupRootNode != null) {
            // Show the warning dialog if the list including Backup node
            this.nodeController = nodeController
            showBackupsWarningDialog(
                handleList = handleList,
                megaNode = backupRootNode,
                nodeType = backupNodeType,
                actionType = ACTION_MENU_BACKUP_SHARE_FOLDER,
                actionBackupNodeCallback = actionBackupNodeCallback,
            )
            return true
        }
        return false
    }

    /**
     * Share the specific Backups Folder
     *
     * @param nodeController The [NodeController]
     * @param megaNode The Backup Node to be shared
     * @param nodeType The Backup Node type
     * @param actionBackupNodeCallback The callback for Backup Node actions
     */
    fun shareBackupsFolder(
        nodeController: NodeController,
        megaNode: MegaNode,
        nodeType: Int,
        actionBackupNodeCallback: ActionBackupNodeCallback,
    ) {
        this.nodeController = nodeController
        showBackupsWarningDialog(
            handleList = null,
            megaNode = megaNode,
            nodeType = nodeType,
            actionType = ACTION_BACKUP_SHARE_FOLDER,
            actionBackupNodeCallback = actionBackupNodeCallback,
        )
    }
}