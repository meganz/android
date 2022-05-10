package mega.privacy.android.app.sync.fileBackups

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.EventConstants.EVENT_MY_BACKUPS_FOLDER_CHANGED
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.interfaces.ActionBackupNodeCallback
import mega.privacy.android.app.main.FileContactListActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_CONFIRM
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_NONE
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_WARNING
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_CANCEL
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE
import mega.privacy.android.app.usecase.MoveNodeUseCase
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.utils.Constants.NAME
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_FAB
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_MOVE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_REMOVE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_COPY_TO_BACKUP
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_MENU_BACKUP_SHARE_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_MOVE_TO_BACKUP
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_DEVICE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_FOLDER
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_FOLDER_CHILD
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE
import mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_ROOT
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showConfirmDialogWithBackup
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showTipDialogWithBackup
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeInList
import mega.privacy.android.app.utils.MegaNodeUtil.getBackupRootNodeByHandle
import mega.privacy.android.app.utils.MegaNodeUtil.isOutShare
import mega.privacy.android.app.utils.MegaNodeUtil.myBackupHandle
import mega.privacy.android.app.utils.Util
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
    private var moveNodeUseCase: MoveNodeUseCase? = null

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
     * Show the warning dialog when acting with "My backups" folder
     *
     * @param handleList The handles list of the nodes that selected
     * @param pNodeBackup Can not be null, for the actions:
     * ACTION_COPY_TO_BACKUP            - The destination node belongs to "My backups"
     * ACTION_BACKUP_REMOVE             - The root of backup node in handleList,
     *                                    otherwise one of the node in handleList
     * ACTION_MOVE_TO_BACKUP            - The destination node belongs to "My backups"
     * ACTION_BACKUP_MOVE               - The root of backup node in handleList,
     *                                    otherwise one of the node in handleList
     * ACTION_MENU_BACKUP_SHARE_FOLDER  - The root of backup node
     * ACTION_BACKUP_SHARE_FOLDER       - The node that select for share.
     * ACTION_BACKUP_FAB                - if the folder is empty, pNodeBackup is the parent node of empty folder,
     *                                    otherwise one of the node in handleList.
     *
     * @param nodeType The type of the backup node - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file:
     *                                  - ACTION_COPY_TO_BACKUP
     *                                  - ACTION_BACKUP_REMOVE
     *                                  - ACTION_MOVE_TO_BACKUP
     *                                  - ACTION_BACKUP_MOVE
     *                                  - ACTION_MENU_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_FAB
     */
    fun actWithBackupTips(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        nodeType: Int,
        actionType: Int,
    ) {
        backupHandleList = handleList
        backupNodeHandle = pNodeBackup.handle
        backupNodeType = nodeType
        backupActionType = actionType
        backupDialogType = BACKUP_DIALOG_SHOW_WARNING

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
                    ACTION_COPY_TO_BACKUP -> if (handleList != null) {
                        val copyHandles = Util.getHandleArray(handleList)
                        nodeController?.copyNodes(copyHandles, pNodeBackup.handle)
                    }

                    ACTION_MOVE_TO_BACKUP -> if (handleList != null) {
                        val handles = Util.getHandleArray(handleList)
                        moveNodeUseCase?.let {
                            it.move(handles, pNodeBackup.handle)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { result: MoveRequestResult?, throwable: Throwable? ->
                                    if (throwable == null && result != null) {
                                        actionBackupListener?.actionBackupResult(
                                            ACTION_MOVE_TO_BACKUP,
                                            OPERATION_EXECUTE,
                                            result,
                                            handles[0]
                                        )
                                    }
                                }
                        }
                    }

                    ACTION_BACKUP_SHARE_FOLDER -> if (isOutShare(pNodeBackup)) {
                        val i = Intent(
                            activity,
                            FileContactListActivity::class.java
                        )
                        i.putExtra(NAME, pNodeBackup.handle)
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
                    else -> {}
                }
            }

            override fun actionConfirmed(
                handleList: ArrayList<Long>?,
                pNodeBackup: MegaNode,
                nodeType: Int,
                actionType: Int,
            ) {
                confirmationActionForBackup(handleList, pNodeBackup, nodeType, actionType)
            }
        }

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
     * Show the confirm dialog when moving or deleting folders with "My backup" folder
     * @param handleList handle list of the nodes that selected
     * @param pNodeBackup Can not be null, for the actions:
     * ACTION_COPY_TO_BACKUP            - The destination node belongs to "My backups"
     * ACTION_BACKUP_REMOVE             - The root of backup node in handleList,
     *                                    otherwise one of the node in handleList
     * ACTION_MOVE_TO_BACKUP            - The destination node belongs to "My backups"
     * ACTION_BACKUP_MOVE               - The root of backup node in handleList,
     *                                    otherwise one of the node in handleList
     * ACTION_MENU_BACKUP_SHARE_FOLDER  - The root of backup node
     * ACTION_BACKUP_SHARE_FOLDER       - The node that select for share.
     * ACTION_BACKUP_FAB                - if the folder is empty, pNodeBackup is the parent node of empty folder,
     *                                    otherwise one of the node in handleList.
     * @param nodeType the type of the backup node - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file:
     *                                  - ACTION_COPY_TO_BACKUP
     *                                  - ACTION_BACKUP_REMOVE
     *                                  - ACTION_MOVE_TO_BACKUP
     *                                  - ACTION_BACKUP_MOVE
     *                                  - ACTION_MENU_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_FAB
     */
    fun confirmationActionForBackup(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        nodeType: Int,
        actionType: Int,
    ) {
        backupHandleList = handleList
        backupNodeHandle = pNodeBackup.handle
        backupNodeType = nodeType
        backupActionType = actionType
        backupDialogType = BACKUP_DIALOG_SHOW_CONFIRM

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
                    ACTION_COPY_TO_BACKUP -> if (handleList != null) {
                        val copyHandles = Util.getHandleArray(handleList)
                        nodeController?.copyNodes(copyHandles, pNodeBackup.handle)
                    }

                    ACTION_BACKUP_REMOVE -> if (handleList != null) {
                        moveNodeUseCase?.let { userCase ->
                            userCase.moveToRubbishBin(handleList)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { result: MoveRequestResult?, throwable: Throwable? ->
                                    if (throwable == null && result != null) {
                                        LiveEventBus.get(
                                            EVENT_MY_BACKUPS_FOLDER_CHANGED,
                                            Boolean::class.java
                                        ).post(true)
                                    }
                                }
                        }
                    }

                    ACTION_MOVE_TO_BACKUP -> if (handleList != null) {
                        val handles = Util.getHandleArray(handleList)
                        moveNodeUseCase?.let { userCase ->
                            userCase.move(handles, pNodeBackup.handle)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { result: MoveRequestResult?, throwable: Throwable? ->
                                    if (throwable == null && result != null) {
                                        actionBackupListener?.actionBackupResult(
                                            ACTION_MOVE_TO_BACKUP,
                                            OPERATION_EXECUTE,
                                            result,
                                            handles[0]
                                        )
                                    }
                                }
                        }
                    }

                    ACTION_BACKUP_MOVE -> if (handleList != null) {
                        nodeController?.chooseLocationToMoveNodes(handleList)
                        LiveEventBus.get(
                            EVENT_MY_BACKUPS_FOLDER_CHANGED,
                            Boolean::class.java
                        ).post(true)
                    }
                    ACTION_BACKUP_FAB -> {
                        actionBackupListener?.actionBackupResult(ACTION_BACKUP_FAB,
                            OPERATION_EXECUTE)
                    }
                    else -> {}
                }
            }

            override fun actionConfirmed(
                handleList: ArrayList<Long>?,
                pNodeBackup: MegaNode,
                nodeType: Int,
                actionType: Int,
            ) {
            }
        }

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
     * Copy the nodes to the folder beneath the backup root folder
     * @param nC The instance of NodeController
     * @param copyHandles Nodes to copy
     * @param toHandle Parent for the new node
     * @return true if the target folder is under backup, else return false
     */
    fun copyNodesToBackups(
        nC: NodeController,
        copyHandles: LongArray,
        toHandle: Long,
    ): Boolean {
        nodeController = nC

        val origHandleList = ArrayList<Long>()
        for (copyHandle in copyHandles) {
            origHandleList.add(copyHandle)
        }

        val destHandleList = ArrayList<Long>()
        destHandleList.add(toHandle)
        val destNodeType = checkBackupNodeTypeInList(megaApi, destHandleList)
        if (destNodeType != BACKUP_NONE) {
            // Warning tips
            actWithBackupTips(
                origHandleList,
                megaApi.getNodeByHandle(toHandle),
                destNodeType,
                ACTION_COPY_TO_BACKUP
            )
            return true
        }
        return false
    }

    /**
     * Remove the the backup root folder or the nodes beneath the backup root folder
     * @param moveNodeUseCase The instance of the Use case for moving MegaNodes
     * @param handleList List of the nodes that selected
     * @return true if the nodes are under backup or backup root node is in the node list, else return false.
     */
    fun removeBackup(moveNodeUseCase: MoveNodeUseCase, handleList: ArrayList<Long>): Boolean {
        this.moveNodeUseCase = moveNodeUseCase
        val nodeType = checkBackupNodeTypeInList(megaApi, handleList)

        // Show the warning dialog if the list including Backup node
        if (nodeType == BACKUP_DEVICE || nodeType == BACKUP_FOLDER || nodeType == BACKUP_FOLDER_CHILD) {
            val subHandle = handleList[0]
            val pSubNode = megaApi.getNodeByHandle(subHandle)
            actWithBackupTips(handleList, pSubNode, nodeType, ACTION_BACKUP_REMOVE)
            return true
        } else if (nodeType == BACKUP_ROOT) {
            actWithBackupTips(
                handleList,
                megaApi.getNodeByHandle(myBackupHandle),
                nodeType,
                ACTION_BACKUP_REMOVE
            )
            return true
        }
        return false
    }

    /**
     * Move the the nodes outside the backup folder to the nodes beneath the backup root folder
     * @param moveNodeUseCase The instance of the Use case for moving MegaNodes
     * @param moveHandles Nodes to move
     * @param toHandle Parent for the new node
     * @return true if moving the the nodes outside the backup folder to the nodes beneath the backup root folder, otherwise return false
     */
    fun moveToBackup(
        moveNodeUseCase: MoveNodeUseCase,
        moveHandles: LongArray,
        toHandle: Long,
    ): Boolean {
        this.moveNodeUseCase = moveNodeUseCase

        // Check the original path
        val origHandleList = ArrayList<Long>()
        for (moveHandle in moveHandles) {
            origHandleList.add(moveHandle)
        }

        val pNode = getBackupRootNodeByHandle(megaApi, origHandleList)
        if (pNode == null) {
            val origNodeType = checkBackupNodeTypeInList(megaApi, origHandleList)
            if (origNodeType == BACKUP_NONE) {
                // Check the destination path
                val destHandleList = ArrayList<Long>()
                destHandleList.add(toHandle)
                val destNodeType = checkBackupNodeTypeInList(megaApi, destHandleList)
                if (destNodeType != BACKUP_NONE) {
                    // Warning tips
                    actWithBackupTips(
                        origHandleList,
                        megaApi.getNodeByHandle(toHandle),
                        destNodeType,
                        ACTION_MOVE_TO_BACKUP
                    )
                    return true
                }
            }
        }
        return false
    }

    /**
     * Move the the backup root folder or the nodes beneath the backup root folder
     * @param nC The instance of NodeController
     * @param handleList List of the nodes that selected
     * @return true if the node list belongs to the backup folder or backup root node is in the node list, else return false.
     */
    fun moveBackup(
        nC: NodeController,
        handleList: ArrayList<Long>,
    ): Boolean {
        nodeController = nC
        if (handleList.size > 0) {
            val nodeType = checkBackupNodeTypeInList(megaApi, handleList)

            // Show the warning dialog if the list including Backup node
            if (nodeType == BACKUP_DEVICE || nodeType == BACKUP_FOLDER || nodeType == BACKUP_FOLDER_CHILD) {
                val handle = handleList[0]
                val p = megaApi.getNodeByHandle(handle)
                actWithBackupTips(handleList, p, nodeType, ACTION_BACKUP_MOVE)
                return true
            } else if (nodeType == BACKUP_ROOT) {
                actWithBackupTips(
                    handleList,
                    megaApi.getNodeByHandle(myBackupHandle),
                    nodeType,
                    ACTION_BACKUP_MOVE
                )
                return true
            } else {
                Timber.d("MyBackup + chooseLocationToPutNodes nodeType = $nodeType")
            }
        }
        return false
    }

    /**
     * Response to the menu item to share the backup folder or folders under the backup
     * @param nC The instance of NodeController
     * @param handleList List of the nodes that selected
     * @return true if the backup node in the list, otherwise return false.
     */
    fun shareBackupFolderInMenu(nC: NodeController, handleList: ArrayList<Long>): Boolean {
        val pNode = getBackupRootNodeByHandle(megaApi, handleList)
        val nodeType = checkBackupNodeTypeInList(megaApi, handleList)

        if (nodeType == BACKUP_NONE && pNode == null) {
            // No backup node in the selected nodes
            return false
        } else if (pNode != null) {
            // Show the warning dialog if the list including Backup node
            nodeController = nC
            actWithBackupTips(handleList, pNode, nodeType, ACTION_MENU_BACKUP_SHARE_FOLDER)
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
     * @return true if the node belongs to the backup folder, else return false.
     */
    fun shareBackupFolder(
        nC: NodeController,
        p: MegaNode,
        nodeType: Int,
        actionType: Int,
    ) {
        nodeController = nC
        actWithBackupTips(null, p, nodeType, actionType)
    }

    /**
     * Share the folder of backup folder or folder under the backup
     * @param nodeList The node list in the current folder
     * @param currentParentHandle The parent handle of the current node list
     * @param actionType Indicates the action to backup folder or file
     * @return true if the node belongs to the backup folder, else return false.
     */
    fun fabForBackup(
        nodeList: List<MegaNode?>,
        currentParentHandle: MegaNode?,
        actionType: Int,
    ): Boolean {
        // isInBackup Indicates if the current node is under "My backup"
        val nodeType: Int = checkSubBackupNode(nodeList, currentParentHandle)

        if (nodeType != BACKUP_ROOT && nodeType != BACKUP_NONE) {
            val parentNode: MegaNode? = getSubBackupParentNode(nodeList, currentParentHandle)
            if (parentNode != null) {
                actWithBackupTips(null, parentNode, nodeType, actionType)
                return true
            }
        }
        return false
    }
}