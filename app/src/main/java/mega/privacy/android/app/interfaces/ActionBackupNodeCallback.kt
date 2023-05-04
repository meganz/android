package mega.privacy.android.app.interfaces

import android.content.DialogInterface
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_NONE
import mega.privacy.android.app.presentation.movenode.MoveRequestResult
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.util.ArrayList

/**
 * This interface is to define what methods the file backup dialog
 * should implement for the operation of backup nodes
 */
interface ActionBackupNodeCallback {

    /**
     * Makes the necessary UI changes after confirm the action.
     * @param handleList The handles list of the nodes that selected
     * @param pNodeBackup Can not be null, for the actions:

     * ACTION_MENU_BACKUP_SHARE_FOLDER  - The root of backup node
     * ACTION_BACKUP_SHARE_FOLDER       - The node that select for share.
     * ACTION_BACKUP_FAB                - if the folder is empty, pNodeBackup is the parent node of empty folder,
     *                                    otherwise one of the node in handleList.
     *
     * @param nodeType The type of the backup node - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file:
     *                                  - ACTION_BACKUP_REMOVE
     *                                  - ACTION_MENU_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_FAB
     */
    fun actionConfirmed(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode?,
        nodeType: Int,
        actionType: Int
    )

    /**
     * Makes the necessary UI changes after execute the action.
     * @param handleList The handles list of the nodes that selected
     * @param pNodeBackup Can not be null, for the actions:

     * ACTION_MENU_BACKUP_SHARE_FOLDER  - The root of backup node
     * ACTION_BACKUP_SHARE_FOLDER       - The node that select for share.
     * ACTION_BACKUP_FAB                - if the folder is empty, pNodeBackup is the parent node of empty folder,
     *                                    otherwise one of the node in handleList.
     *
     * @param nodeType The type of the backup node - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the action to backup folder or file:
     *                                  - ACTION_BACKUP_REMOVE
     *                                  - ACTION_MENU_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_SHARE_FOLDER
     *                                  - ACTION_BACKUP_FAB
     */
    fun actionExecute(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode?,
        nodeType: Int,
        actionType: Int
    )
    /**
     * Makes the necessary UI changes after cancel the action.
     * @param dialog The warning dialog
     * @param actionType Indicates the action to backup folder or file:
     *              - ACTION_MENU_BACKUP_SHARE_FOLDER
     *              - ACTION_BACKUP_SHARE_FOLDER
     *              - ACTION_BACKUP_FAB
     */
    fun actionCancel(dialog: DialogInterface?, actionType: Int)
}

/**
 * This interface is to define what methods the activity
 * should implement for getting results from file backup operation
 */
interface ActionBackupListener {
    /**
     * Makes the necessary UI changes for the result of backup operation.
     * @param actionType Indicates the action to backup folder or file
     * @param operationType Indicates the operation type
     * @param result The movement request
     * @param handle The node handle
     */
    fun actionBackupResult(actionType: Int, operationType: Int = OPERATION_NONE, result: MoveRequestResult? = null, handle: Long = INVALID_HANDLE)
}