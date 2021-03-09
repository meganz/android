package mega.privacy.android.app.utils

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.saver.AutoPlayInfo
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.CopyNodeListener
import mega.privacy.android.app.listeners.MoveNodeListener
import mega.privacy.android.app.listeners.RenameNodeListener
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.setLocalIntentParams
import mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.getMediaIntent
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.io.File
import java.util.*


class MegaNodeUtilKt {
    companion object {

        /**
         * Start FileExplorerActivityLollipop to select folder to move nodes.
         *
         * @param activity current Android activity
         * @param handles handles to move
         */
        @JvmStatic
        fun selectFolderToMove(activity: Activity, handles: LongArray) {
            val intent = Intent(activity, FileExplorerActivityLollipop::class.java)
            intent.action = FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER
            intent.putExtra(INTENT_EXTRA_KEY_MOVE_FROM, handles)
            activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_MOVE)
        }

        /**
         * Handle activity result of REQUEST_CODE_SELECT_FOLDER_TO_MOVE.
         *
         * @param requestCode requestCode parameter of onActivityResult
         * @param resultCode resultCode parameter of onActivityResult
         * @param data data parameter of onActivityResult
         * @param snackbarShower interface to show snackbar
         */
        @JvmStatic
        fun handleSelectFolderToMoveResult(
            requestCode: Int, resultCode: Int, data: Intent?, snackbarShower: SnackbarShower
        ): List<Long> {
            if (requestCode != REQUEST_CODE_SELECT_FOLDER_TO_MOVE
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
         * Start FileExplorerActivityLollipop to select folder to copy nodes.
         *
         * @param activity current Android activity
         * @param handles handles to copy
         */
        @JvmStatic
        fun selectFolderToCopy(activity: Activity, handles: LongArray) {
            if (MegaApplication.getInstance().storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
                AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
                return
            }

            val intent = Intent(activity, FileExplorerActivityLollipop::class.java)
            intent.action = FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER
            intent.putExtra(INTENT_EXTRA_KEY_COPY_FROM, handles)
            activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_COPY)
        }

        /**
         * Handle activity result of REQUEST_CODE_SELECT_FOLDER_TO_COPY.
         *
         * @param requestCode requestCode parameter of onActivityResult
         * @param resultCode resultCode parameter of onActivityResult
         * @param data data parameter of onActivityResult
         * @param snackbarShower interface to show snackbar
         * @param activityLauncher interface to start activity
         */
        @JvmStatic
        fun handleSelectFolderToCopyResult(
            requestCode: Int, resultCode: Int, data: Intent?, snackbarShower: SnackbarShower,
            activityLauncher: ActivityLauncher
        ): Boolean {
            if (requestCode != REQUEST_CODE_SELECT_FOLDER_TO_COPY
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
            snackbarShower: SnackbarShower
        ) {
            val mime = MimeTypeList.typeForName(autoPlayInfo.nodeName)
            when {
                mime.isZip -> {
                    val zipFile = File(autoPlayInfo.localPath)

                    val intentZip = Intent(context, ZipBrowserActivityLollipop::class.java)
                    intentZip.putExtra(
                        ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, zipFile.absolutePath
                    )
                    intentZip.putExtra(
                        ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, autoPlayInfo.nodeHandle
                    )

                    activityLauncher.launchActivity(intentZip)
                }
                mime.isPdf -> {
                    val pdfIntent = Intent(context, PdfViewerActivityLollipop::class.java)
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
                mime.isVideoReproducible || mime.isAudio -> {
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
                            sendFile(context, autoPlayInfo, activityLauncher, snackbarShower)
                        }
                    }
                }
                else -> {
                    try {
                        val viewIntent = Intent(Intent.ACTION_VIEW)

                        if (!setLocalIntentParams(
                                context, autoPlayInfo.nodeName, viewIntent,
                                autoPlayInfo.localPath, false, snackbarShower
                            )
                        ) {
                            return
                        }

                        viewIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        if (isIntentAvailable(context, viewIntent)) {
                            activityLauncher.launchActivity(viewIntent)
                        } else {
                            sendFile(context, autoPlayInfo, activityLauncher, snackbarShower)
                        }
                    } catch (e: Exception) {
                        snackbarShower.showSnackbar(getString(R.string.general_already_downloaded))
                    }
                }
            }
        }

        /**
         * Create an Intent with ACTION_SEND for an auto play file.
         *
         * @param context Android context
         * @param autoPlayInfo auto play file info
         * @param activityLauncher interface to launch activity
         * @param snackbarShower interface to show snackbar
         */
        private fun sendFile(
            context: Context,
            autoPlayInfo: AutoPlayInfo,
            activityLauncher: ActivityLauncher,
            snackbarShower: SnackbarShower
        ) {
            val intentShare = Intent(Intent.ACTION_SEND)

            if (!setLocalIntentParams(
                    context, autoPlayInfo.nodeName, intentShare,
                    autoPlayInfo.localPath, false, snackbarShower
                )
            ) {
                return
            }

            intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (isIntentAvailable(context, intentShare)) {
                activityLauncher.launchActivity(intentShare)
            }
        }
    }
}
