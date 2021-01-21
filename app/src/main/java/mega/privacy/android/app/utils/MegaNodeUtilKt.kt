package mega.privacy.android.app.utils

import android.app.Activity.RESULT_OK
import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.CopyNodeListener
import mega.privacy.android.app.listeners.MoveNodeListener
import mega.privacy.android.app.listeners.RenameNodeListener
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.util.*

class MegaNodeUtilKt {
    companion object {

        /**
         * Handle activity result of REQUEST_CODE_SELECT_MOVE_FOLDER.
         *
         * @param requestCode requestCode parameter of onActivityResult
         * @param resultCode resultCode parameter of onActivityResult
         * @param data data parameter of onActivityResult
         * @param snackbarShower interface to show snackbar
         */
        @JvmStatic
        fun handleSelectMoveFolderResult(
            requestCode: Int, resultCode: Int, data: Intent?, snackbarShower: SnackbarShower
        ): List<Long> {
            if (requestCode != REQUEST_CODE_SELECT_MOVE_FOLDER
                || resultCode != RESULT_OK || data == null
            ) {
                return emptyList()
            }

            val moveHandles = data.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)

            if (moveHandles == null || moveHandles.isEmpty()) {
                return emptyList()
            }

            val megaApp = MegaApplication.getInstance()
            val megaApi = megaApp.megaApi

            val toHandle = data.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
            val parent = megaApi.getNodeByHandle(toHandle) ?: return emptyList()

            val listener = MoveNodeListener(snackbarShower, megaApp)
            val result = ArrayList<Long>()

            for (handle in moveHandles) {
                val node = megaApi.getNodeByHandle(handle)

                if (node != null) {
                    result.add(handle)
                    megaApi.moveNode(node, parent, listener)
                }
            }

            return result
        }

        /**
         * Handle activity result of REQUEST_CODE_SELECT_COPY_FOLDER.
         *
         * @param requestCode requestCode parameter of onActivityResult
         * @param resultCode resultCode parameter of onActivityResult
         * @param data data parameter of onActivityResult
         * @param snackbarShower interface to show snackbar
         * @param activityLauncher interface to start activity
         */
        @JvmStatic
        fun handleSelectCopyFolderResult(
            requestCode: Int, resultCode: Int, data: Intent?, snackbarShower: SnackbarShower,
            activityLauncher: ActivityLauncher
        ): Boolean {
            if (requestCode != REQUEST_CODE_SELECT_COPY_FOLDER
                || resultCode != RESULT_OK || data == null
            ) {
                return false
            }

            val copyHandles = data.getLongArrayExtra(INTENT_EXTRA_KEY_COPY_HANDLES)

            if (copyHandles == null || copyHandles.isEmpty()) {
                return false
            }

            val megaApp = MegaApplication.getInstance()
            val megaApi = megaApp.megaApi

            val toHandle = data.getLongExtra(INTENT_EXTRA_KEY_COPY_TO, INVALID_HANDLE)
            val parent = megaApi.getNodeByHandle(toHandle) ?: return false

            val listener = CopyNodeListener(snackbarShower, activityLauncher, megaApp)

            for (handle in copyHandles) {
                val node = megaApi.getNodeByHandle(handle)

                if (node != null) {
                    megaApi.copyNode(node, parent, listener)
                }
            }

            return true
        }

        /**
         * Move a node into rubbish bin.
         *
         * @param node node to move
         * @param snackbarShower interface to show snackbar
         */
        @JvmStatic
        fun moveNodeToRubbishBin(node: MegaNode, snackbarShower: SnackbarShower) {
            val megaApp = MegaApplication.getInstance()
            val megaApi = megaApp.megaApi

            megaApi.moveNode(node, megaApi.rubbishNode, MoveNodeListener(snackbarShower, megaApp))
        }

        /**
         * Rename a node.
         *
         * @param node node to rename
         * @param newName new name of the node
         * @param snackbarShower interface to show snackbar
         */
        @JvmStatic
        fun renameNode(node: MegaNode, newName: String, snackbarShower: SnackbarShower) {
            val megaApp = MegaApplication.getInstance()
            val megaApi = megaApp.megaApi

            megaApi.renameNode(node, newName, RenameNodeListener(snackbarShower, megaApp))
        }

    }
}
