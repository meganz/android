package mega.privacy.android.app.interfaces

import android.content.DialogInterface
import mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_NONE
import mega.privacy.android.app.usecase.data.MoveRequestResult
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
     */
    fun actionConfirmed(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        nodeType: Int,
        actionType: Int
    )

    /**
     * Makes the necessary UI changes after execute the action.
     */
    fun actionExecute(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        nodeType: Int,
        actionType: Int
    )
    /**
     * Makes the necessary UI changes after cancel the action.
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