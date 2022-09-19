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
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_CONFIRM
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_NONE
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_WARNING
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_CANCEL
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_FAB
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_MENU_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_DEVICE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_FOLDER_CHILD
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_ROOT
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showConfirmDialogWithBackup
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showTipDialogWithBackup
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeInList
import mega.privacy.android.app.utils.MegaNodeUtil.getBackupRootNodeByHandle
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.*

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
        const val BACKUP_DIALOG_SHOW_CONFIRM = 1
    }

    object OperationType {
        const val OPERATION_NONE = -1
        const val OPERATION_CANCEL = 0
        const val OPERATION_CONFIRMED = 1
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
            pNodeBackup: MegaNode,
            nodeType: Int,
            actionType: Int,
        ) {
            initBackupWarningState()
            actionBackupListener?.actionBackupResult(actionType, OPERATION_EXECUTE)
        }

        override fun actionConfirmed(
            handleList: ArrayList<Long>?,
            pNodeBackup: MegaNode,
            nodeType: Int,
            actionType: Int,
        ) = Unit
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
            pNodeBackup: MegaNode,
            nodeType: Int,
            actionType: Int,
        ) {
            initBackupWarningState()
            when (actionType) {
                MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER -> if (MegaNodeUtil.isOutShare(
                        pNodeBackup)
                ) {
                    val i = Intent(
                        activity,
                        FileContactListActivity::class.java
                    )
                    i.putExtra(Constants.NAME, pNodeBackup.handle)
                    activity.startActivity(i)
                } else {
                    nodeController?.selectContactToShareFolder(pNodeBackup)
                }

                ACTION_MENU_BACKUP_SHARE_FOLDER -> nodeController?.selectContactToShareFolders(
                    handleList
                )

                ACTION_BACKUP_FAB -> {
                    actionBackupListener?.actionBackupResult(ACTION_BACKUP_FAB,
                        OPERATION_EXECUTE)
                }
            }
        }

        override fun actionConfirmed(
            handleList: ArrayList<Long>?,
            pNodeBackup: MegaNode,
            nodeType: Int,
            actionType: Int,
        ) {
            confirmationActionForBackup(
                handleList = handleList,
                pNodeBackup = pNodeBackup,
                nodeType = nodeType,
                actionType = actionType,
                actionBackupNodeCallback = defaultActionBackupNodeCallback,
            )
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
     * Check the type of the node in Backup folder (include empty folder).
     * @param megaNodes The nodes in the folder
     * @param currentParentHandle The parent node
     * @return The type of node
     */
    fun checkSubBackupNode(megaNodes: List<MegaNode?>?, currentParentHandle: MegaNode?): Int {
        if (megaNodes == null) return BACKUP_NONE

        if (megaNodes.isNotEmpty()) {
            val handleList = megaNodes.mapNotNull { it?.handle }
            return checkBackupNodeTypeInList(megaApi, handleList)
        } else {
            // for empty folder
            return if (currentParentHandle != null) {
                val nodeType = checkBackupNodeTypeByHandle(megaApi, currentParentHandle)
                Timber.d("nodeType = $nodeType")
                return when (nodeType) {
                    BACKUP_ROOT -> BACKUP_DEVICE
                    BACKUP_DEVICE -> BACKUP_FOLDER
                    BACKUP_FOLDER, BACKUP_FOLDER_CHILD -> BACKUP_FOLDER_CHILD
                    else -> BACKUP_NONE
                }
            } else BACKUP_NONE
        }
    }

    /**
     * Get the parent node in Backup folder (include empty folder).
     * @param megaNodes The nodes in the folder
     * @param currentParentHandle The parent node of empty folder
     * @return The parent node or null
     */
    fun getSubBackupParentNode(
        megaNodes: List<MegaNode?>?,
        currentParentHandle: MegaNode?,
    ): MegaNode? = megaNodes?.firstNotNullOfOrNull { it?.let { megaApi.getParentNode(it) } }
        ?: currentParentHandle


    /**
     * Show the warning dialog when performing an action with "My backups" folder
     *
     * @param handleList The handles list of the nodes that selected
     * @param pNodeBackup Can not be null, for the actions:

     * ACTION_MENU_BACKUP_SHARE_FOLDER  - The root of backup node
     * ACTION_BACKUP_SHARE_FOLDER       - The node that select for share.
     * ACTION_BACKUP_FAB                - if the folder is empty, pNodeBackup is the parent node of empty folder,
     *                                    otherwise one of the node in handleList.
     *
     * @param nodeType The type of the backup node - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file:
     *                                  - ACTION_MENU_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_FAB
     * @param actionBackupNodeCallback Callback for Backup Node Actions.
     */
    fun actWithBackupTips(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        nodeType: Int,
        actionType: Int,
        actionBackupNodeCallback: ActionBackupNodeCallback,
    ) {
        backupHandleList = handleList
        backupNodeHandle = pNodeBackup.handle
        backupNodeType = nodeType
        backupActionType = actionType
        backupDialogType = BACKUP_DIALOG_SHOW_WARNING

        backupWarningDialog = showTipDialogWithBackup(
            activity,
            actionBackupNodeCallback,
            handleList,
            pNodeBackup,
            nodeType,
            actionType
        )
    }

    /**
     * Show the confirm dialog when performing an action with "My backups" folder
     * @param handleList handle list of the nodes that selected
     * @param pNodeBackup Can not be null, for the actions:

     * ACTION_MENU_BACKUP_SHARE_FOLDER  - The root of backup node
     * ACTION_BACKUP_SHARE_FOLDER       - The node that select for share.
     * ACTION_BACKUP_FAB                - if the folder is empty, pNodeBackup is the parent node of empty folder,
     *                                    otherwise one of the node in handleList.
     * @param nodeType the type of the backup node - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file:
     *                                  - ACTION_MENU_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_FAB
     * @param actionBackupNodeCallback Callback for Backup Node Actions.
     */
    fun confirmationActionForBackup(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        nodeType: Int,
        actionType: Int,
        actionBackupNodeCallback: ActionBackupNodeCallback,
    ) {
        backupHandleList = handleList
        backupNodeHandle = pNodeBackup.handle
        backupNodeType = nodeType
        backupActionType = actionType
        backupDialogType = BACKUP_DIALOG_SHOW_CONFIRM

        backupWarningDialog = showConfirmDialogWithBackup(
            activity,
            actionBackupNodeCallback,
            handleList,
            pNodeBackup,
            nodeType,
            actionType
        )
    }

    /**
     * Response to the menu item to share the backup folder or folders under the backup
     * @param nC The instance of NodeController
     * @param handleList List of the nodes that selected
     * @param actionBackupNodeCallback Callback for Backup Node actions
     *
     * @return true if the backup node in the list, otherwise return false.
     */
    fun shareBackupFolderInMenu(
        nC: NodeController,
        handleList: ArrayList<Long>,
        actionBackupNodeCallback: ActionBackupNodeCallback,
    ): Boolean {
        val pNode = getBackupRootNodeByHandle(megaApi, handleList)
        val nodeType = checkBackupNodeTypeInList(megaApi, handleList)

        if (nodeType == BACKUP_NONE && pNode == null) {
            // No backup node in the selected nodes
            return false
        } else if (pNode != null) {
            // Show the warning dialog if the list including Backup node
            nodeController = nC
            actWithBackupTips(
                handleList = handleList,
                pNodeBackup = pNode,
                nodeType = nodeType,
                actionType = ACTION_MENU_BACKUP_SHARE_FOLDER,
                actionBackupNodeCallback = actionBackupNodeCallback,
            )
            return true
        }
        return false
    }

    /**
     * Share the folder of backup folder or folder under the backup
     * @param nC The instance of NodeController
     * @param p The nodes that selected
     * @param nodeType The type of the node [p]
     * @param actionType Indicates the action to backup folder or file
     * @param actionBackupNodeCallback Callback for Backup Node actions
     *
     * @return true if the node belongs to the backup folder, else return false.
     */
    fun shareBackupFolder(
        nC: NodeController,
        p: MegaNode,
        nodeType: Int,
        actionType: Int,
        actionBackupNodeCallback: ActionBackupNodeCallback,
    ) {
        nodeController = nC
        actWithBackupTips(
            handleList = null,
            pNodeBackup = p,
            nodeType = nodeType,
            actionType = actionType,
            actionBackupNodeCallback = actionBackupNodeCallback,
        )
    }

    /**
     * Share the folder of backup folder or folder under the backup
     * @param nodeList The node list in the current folder
     * @param currentParentHandle The parent handle of the current node list
     * @param actionType Indicates the action to backup folder or file
     * @param actionBackupNodeCallback Callback for Backup Node actions
     *
     * @return true if the node belongs to the backup folder, else return false.
     */
    fun fabForBackup(
        nodeList: List<MegaNode?>,
        currentParentHandle: MegaNode?,
        actionType: Int,
        actionBackupNodeCallback: ActionBackupNodeCallback,
    ): Boolean {
        // isInBackup Indicates if the current node is under "My backup"
        val nodeType: Int = checkSubBackupNode(nodeList, currentParentHandle)

        if (nodeType != BACKUP_ROOT && nodeType != BACKUP_NONE) {
            val parentNode: MegaNode? = getSubBackupParentNode(nodeList, currentParentHandle)
            if (parentNode != null) {
                actWithBackupTips(
                    handleList = null,
                    pNodeBackup = parentNode,
                    nodeType = nodeType,
                    actionType = actionType,
                    actionBackupNodeCallback = actionBackupNodeCallback,
                )
                return true
            }
        }
        return false
    }
}