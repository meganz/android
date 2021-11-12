package mega.privacy.android.app.interfaces

import android.content.DialogInterface
import nz.mega.sdk.MegaNode
import java.util.ArrayList

interface ActionBackupNodeCallback {


    /**
     * Makes the necessary UI changes after confirm the action.
     */
    fun actionConfirmed(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        isRootBackup: Boolean,
        actionType: Int
    )

    /**
     * Makes the necessary UI changes after execute the action.
     */
    fun actionExecute(
        handleList: ArrayList<Long>?,
        pNodeBackup: MegaNode,
        isRootBackup: Boolean,
        actionType: Int
    )
    /**
     * Makes the necessary UI changes after cancel the action.
     */
    fun actionCancel(dialog: DialogInterface?, actionType: Int)
}
