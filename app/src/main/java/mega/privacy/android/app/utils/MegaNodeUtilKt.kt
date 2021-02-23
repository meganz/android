package mega.privacy.android.app.utils

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.CopyNodeListener
import mega.privacy.android.app.listeners.MoveNodeListener
import mega.privacy.android.app.listeners.RenameNodeListener
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.io.File
import java.util.*

class MegaNodeUtilKt {
    companion object {

        /**
         * Start FileExplorerActivityLollipop to select move folder.
         *
         * @param activity current Android activity
         * @param handles handles to move
         */
        @JvmStatic
        fun selectMoveFolder(activity: Activity, handles: LongArray) {
            val intent = Intent(activity, FileExplorerActivityLollipop::class.java)
            intent.action = FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER
            intent.putExtra(INTENT_EXTRA_KEY_MOVE_FROM, handles)
            activity.startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER)
        }

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
         * Start FileExplorerActivityLollipop to select copy folder.
         *
         * @param activity current Android activity
         * @param handles handles to copy
         */
        @JvmStatic
        fun selectCopyFolder(activity: Activity, handles: LongArray) {
            if (MegaApplication.getInstance().storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
                AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
                return
            }

            val intent = Intent(activity, FileExplorerActivityLollipop::class.java)
            intent.action = FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER
            intent.putExtra(INTENT_EXTRA_KEY_COPY_FROM, handles)
            activity.startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER)
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
            handle: Long
        ): LocationInfo? {
            val app = MegaApplication.getInstance()
            val dbHandler = DatabaseHandler.getDbHandler(app)
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
                        getString(R.string.section_saved_for_offline_new)
                    }
                    parentName == OfflineUtils.OFFLINE_DIR -> {
                        getString(R.string.section_saved_for_offline_new)
                    }
                    else -> {
                        parentName + " (" + getString(R.string.section_saved_for_offline_new) + ")"
                    }
                }

                return LocationInfo(location, offlineParentPath = node.path)
            } else {
                val node = megaApi.getNodeByHandle(handle) ?: return null

                val parent = megaApi.getParentNode(node)
                val topAncestor = MegaNodeUtil.getRootParentNode(node)

                val inCloudDrive = topAncestor.handle == megaApi.rootNode.handle
                        || topAncestor.handle == megaApi.rubbishNode.handle
                        || topAncestor.handle == megaApi.inboxNode.handle

                val location = when {
                    fromIncomingShare -> {
                        if (parent != null) {
                            parent.name + " (" + getString(R.string.tab_incoming_shares) + ")"
                        } else {
                            getString(R.string.tab_incoming_shares)
                        }
                    }
                    parent == null -> {
                        getString(R.string.tab_incoming_shares)
                    }
                    inCloudDrive -> {
                        if (topAncestor.handle == parent.handle) {
                            getTranslatedNameForParentNode(megaApi, topAncestor)
                        } else {
                            parent.name + " (" +
                                    getTranslatedNameForParentNode(megaApi, topAncestor) + ")"
                        }
                    }
                    else -> {
                        parent.name + " (" + getString(R.string.tab_incoming_shares) + ")"
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
            val intent = Intent(activity, ManagerActivityLollipop::class.java)

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
            parent: MegaNode
        ): String {
            return when (parent.handle) {
                megaApi.rootNode.handle -> getString(R.string.section_cloud_drive)
                megaApi.rubbishNode.handle -> getString(R.string.section_rubbish_bin)
                megaApi.inboxNode.handle -> getString(R.string.section_inbox)
                else -> parent.name
            }
        }
    }
}
