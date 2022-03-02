package mega.privacy.android.app.components.saver

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.*
import mega.privacy.android.app.interfaces.*
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.*
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode.PICK_FOLDER
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtil.autoPlayNode
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.app.utils.Util.storeDownloadLocationIfNeeded
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.nodeListToArray
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import java.util.concurrent.Callable

/**
 * A class that encapsulate all the procedure of saving nodes into device,
 * including choose save to internal storage or external sdcard,
 * choose save path, check download size, check other apps that could open this file, etc,
 * the final step that really download the node into a file is handled in sub-classes of Saving,
 * by implementing the abstract doDownload function.
 *
 * The initiation API of save should also be added by sub-classes, because it's usually
 * related with the final download step.
 *
 * It simplifies code in app/fragment where nodes need to be saved.
 */
class NodeSaver(
    private val activityLauncher: ActivityLauncher,
    private val permissionRequester: PermissionRequester,
    private val snackbarShower: SnackbarShower,
    private val confirmDialogShower: (message: String, onConfirmed: (Boolean) -> Unit) -> Unit
) {
    private val compositeDisposable = CompositeDisposable()

    private val app = MegaApplication.getInstance()
    private val megaApi = app.megaApi
    private val megaApiFolder = app.megaApiFolder
    private val dbHandler = DatabaseHandler.getDbHandler(app)

    private var saving : Saving = Saving.Companion.NOTHING

    /**
     * Save an offline node into device.
     *
     * @param handle handle of the offline node to save
     * @param fromMediaViewer whether this download is from media viewer
     */
    @JvmOverloads
    fun saveOfflineNode(
        handle: Long,
        fromMediaViewer: Boolean = false
    ) {
        val node = dbHandler.findByHandle(handle) ?: return
        saveOfflineNodes(listOf(node), fromMediaViewer)
    }

    /**
     * Save an offline node into device.
     *
     * @param node the offline node to save
     * @param fromMediaViewer whether this download is from media viewer
     */
    @JvmOverloads
    fun saveOfflineNode(
        node: MegaOffline,
        fromMediaViewer: Boolean = false
    ) {
        saveOfflineNodes(listOf(node), fromMediaViewer)
    }

    /**
     * Save offline nodes into device.
     *
     * @param nodes the offline nodes to save
     * @param fromMediaViewer whether this download is from media viewer
     */
    @JvmOverloads
    fun saveOfflineNodes(
        nodes: List<MegaOffline>,
        fromMediaViewer: Boolean = false
    ) {
        save {
            var totalSize = 0L
            for (node in nodes) {
                totalSize += getTotalSize(getOfflineFile(app, node))
            }
            OfflineSaving(totalSize, nodes, fromMediaViewer)
        }
    }

    /**
     * Save a MegaNode into device.
     *
     * @param handle the handle of node to save
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param needSerialize whether this download need serialize
     */
    @JvmOverloads
    fun saveHandle(
        handle: Long,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        needSerialize: Boolean = false
    ) {
        saveHandles(
            listOf(handle),
            highPriority,
            isFolderLink,
            fromMediaViewer,
            needSerialize
        )
    }

    /**
     * Save a list of MegaNode into device.
     * No matter if the list contains only files, or files and folders.
     *
     * @param handles the handle list of nodes to save
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param needSerialize whether this download need serialize
     */
    @JvmOverloads
    fun saveHandles(
        handles: List<Long>,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        needSerialize: Boolean = false
    ) {
        save {
            val nodes = ArrayList<MegaNode>()
            val api = if (isFolderLink) megaApiFolder else megaApi

            for (handle in handles) {
                val node = api.getNodeByHandle(handle)
                if (node != null) {
                    nodes.add(node)
                }
            }

            MegaNodeSaving(
                totalSize = nodesTotalSize(nodes),
                highPriority = highPriority,
                isFolderLink = isFolderLink,
                nodes = nodes,
                fromMediaViewer = fromMediaViewer,
                needSerialize = needSerialize
            )
        }
    }

    /**
     * Save a MegaNode into device.
     *
     * @param node node to save
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param needSerialize whether this download need serialize
     */
    @JvmOverloads
    fun saveNode(
        node: MegaNode,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        needSerialize: Boolean = false
    ) {
        saveNodes(listOf(node), highPriority, isFolderLink, fromMediaViewer, needSerialize)
    }

    /**
     * Save a list of MegaNodeList into device.
     *
     * @param nodeLists MegaNodeLists to save
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param needSerialize whether this download need serialize
     */
    @JvmOverloads
    fun saveNodeLists(
        nodeLists: List<MegaNodeList>,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        needSerialize: Boolean = false
    ) {
        save {
            val nodes = ArrayList<MegaNode>()
            for (nodeList in nodeLists) {
                val array = nodeListToArray(nodeList)
                if (array != null) {
                    nodes.addAll(array)
                }
            }

            MegaNodeSaving(
                nodesTotalSize(nodes), highPriority, isFolderLink, nodes, fromMediaViewer,
                needSerialize
            )
        }
    }

    /**
     * Save a list of MegaNode into device.
     *
     * @param nodes nodes to save
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param needSerialize whether this download need serialize
     * @param downloadByTap whether this download is triggered by tap
     */
    @JvmOverloads
    fun saveNodes(
        nodes: List<MegaNode>,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        needSerialize: Boolean = false,
        downloadByTap: Boolean = false
    ) {
        save {
            MegaNodeSaving(
                nodesTotalSize(nodes), highPriority, isFolderLink, nodes, fromMediaViewer,
                needSerialize, downloadByTap = downloadByTap
            )
        }
    }

    /**
     * Save a MegaNode into device.
     *
     * @param node node to save
     * @param parentPath parent path
     */
    fun saveNode(node: MegaNode, parentPath: String) {
        Completable
            .fromCallable(Callable {
                this.saving = MegaNodeSaving(
                    node.size,
                    highPriority = false,
                    isFolderLink = false,
                    nodes = listOf(node),
                    fromMediaViewer = false,
                    needSerialize = false
                )

                if (lackPermission()) {
                    return@Callable
                }

                checkSizeBeforeDownload(parentPath)
            })
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { logErr("NodeSaver saveNode") })
            .addTo(compositeDisposable)
    }

    /**
     * Download voice clip.
     *
     * @param nodeList voice clip to download
     */
    fun downloadVoiceClip(nodeList: MegaNodeList) {
        Completable
            .fromCallable(Callable {
                val nodes = nodeListToArray(nodeList)
                if (nodes == null || nodes.isEmpty()) {
                    return@Callable
                }

                val parentPath =
                    buildVoiceClipFile(app, nodes[0].name)?.parentFile?.path ?: return@Callable

                val totalSize = nodesTotalSize(nodes)

                if (notEnoughSpace(parentPath, totalSize)) {
                    return@Callable
                }

                val voiceClipSaving = MegaNodeSaving(
                    totalSize,
                    highPriority = true,
                    isFolderLink = false,
                    nodes = nodes,
                    fromMediaViewer = false,
                    needSerialize = true,
                    isVoiceClip = true
                )

                voiceClipSaving.doDownload(
                    megaApi, megaApiFolder, parentPath, false, null, null
                )
            })
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { logErr("NodeSaver downloadVoiceClip") })
            .addTo(compositeDisposable)
    }

    /**
     * Save an Uri into device.
     *
     * @param uri uri to save
     * @param name name of this uri
     * @param size size of this uri content
     * @param fromMediaViewer whether this download is from media viewer
     */
    @JvmOverloads
    fun saveUri(
        uri: Uri,
        name: String,
        size: Long,
        fromMediaViewer: Boolean = false,
    ) {
        save {
            UriSaving(uri, name, size, fromMediaViewer)
        }
    }

    /**
     * Handle app result from FileStorageActivityLollipop launched by requestLocalFolder,
     * and take actions according to the state and result.
     *
     * It should be called in onActivityResult.
     *
     * @param activity      Activity required to show a confirmation dialog.
     * @param requestCode   The requestCode from onActivityResult.
     * @param resultCode    The resultCode from onActivityResult.
     * @param intent        The intent from onActivityResult.
     * @return whether NodeSaver handles this result, if this method return false,
     * fragment/app should handle the result by other code.
     */
    fun handleActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ): Boolean {
        if (saving == Saving.Companion.NOTHING) {
            return false
        }

        if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == Activity.RESULT_OK) {
            logDebug("REQUEST_CODE_SELECT_LOCAL_FOLDER")
            if (intent == null) {
                logWarning("Intent null")
                return false
            }

            val parentPath = intent.getStringExtra(EXTRA_PATH)
            if (parentPath == null) {
                logWarning("parentPath null")
                return false
            }

            if (dbHandler.credentials != null && dbHandler.askSetDownloadLocation && activity is BaseActivity) {
                activity.showConfirmationSaveInSameLocation(parentPath)
            }

            Completable
                .fromCallable {
                    storeDownloadLocationIfNeeded(parentPath)
                    checkSizeBeforeDownload(parentPath)
                }
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { logErr("NodeSaver handleActivityResult") })
                .addTo(compositeDisposable)

            return true
        } else if (requestCode == REQUEST_CODE_TREE) {
            if (intent == null) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: result intent is null")

                val message = if (resultCode != Activity.RESULT_OK) {
                    getString(R.string.download_requires_permission)
                } else {
                    getString(R.string.no_external_SD_card_detected)
                }

                snackbarShower.showSnackbar(message)

                return false
            }

            val uri = intent.data
            if (uri == null) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: tree uri is null!")
                return false
            }

            val pickedDir = DocumentFile.fromTreeUri(app, uri)
            if (pickedDir == null || !pickedDir.canWrite()) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: pickedDir not writable")
                return false
            }

            dbHandler.sdCardUri = uri.toString()

            val parentPath = getFullPathFromTreeUri(uri, app)
            if (parentPath == null) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: parentPath is null")
                return false
            }

            Completable.fromCallable { checkSizeBeforeDownload(parentPath) }
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { logErr("NodeSaver handleActivityResult") })
                .addTo(compositeDisposable)

            return true
        }

        return false
    }

    /**
     * Handle request permission result, and take actions according to the state and result.
     *
     * It should be called in onRequestPermissionsResult (but this doesn't mean NodeSaver should be
     * owned by an app).
     *
     * @param requestCode the requestCode from onRequestPermissionsResult
     * @return whether NodeSaver handles this result, if this method return false,
     * app should handle the result by other code.
     */
    fun handleRequestPermissionsResult(requestCode: Int): Boolean {
        if (requestCode != REQUEST_WRITE_STORAGE) {
            return false
        }

        if (hasWriteExternalStoragePermission()) {
            Completable
                .fromCallable { doSave() }
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { logErr("NodeSaver handleRequestPermissionsResult") })
                .addTo(compositeDisposable)
        }

        return true
    }

    /**
     * Save instance state, should be called from onSaveInstanceState of the owning
     * activity/fragment.
     *
     * @param outState outState param of onSaveInstanceState
     */
    fun saveState(outState: Bundle) {
        outState.putParcelable(STATE_KEY_SAVING, saving)
    }

    /**
     * Restore instance state, should be called from onCreate of the owning
     * activity/fragment.
     *
     * @param savedInstanceState savedInstanceState param of onCreate
     */
    fun restoreState(savedInstanceState: Bundle) {
        val oldSaving = savedInstanceState.getParcelable<Saving>(STATE_KEY_SAVING) ?: return
        saving = oldSaving
    }

    /**
     * Clear all internal state and cancel all flying operation, should be called
     * in onDestroy lifecycle callback.
     */
    fun destroy() {
        compositeDisposable.dispose()
    }

    private fun save(savingProducer: () -> Saving?) {
        Completable
            .fromCallable(Callable {
                val saving = savingProducer() ?: return@Callable
                this.saving = saving

                if (lackPermission()) {
                    return@Callable
                }

                doSave()
            })
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { logErr("NodeSaver save") })
            .addTo(compositeDisposable)
    }

    private fun doSave() {
        if (Util.askMe()) {
            requestLocalFolder(null, activityLauncher)
        } else {
            checkSizeBeforeDownload(getDownloadLocation())
        }
    }

    private fun requestLocalFolder(
        prompt: String?, activityLauncher: ActivityLauncher
    ) {
        val intent = Intent(PICK_FOLDER.action)
        intent.putExtra(PICK_FOLDER_TYPE, PickFolderType.DOWNLOAD_FOLDER.folderType)
        intent.setClass(app, FileStorageActivityLollipop::class.java)

        if (prompt != null) {
            intent.putExtra(EXTRA_PROMPT, prompt)
        }

        activityLauncher.launchActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER)
    }

    private fun nodesTotalSize(nodes: List<MegaNode>): Long {
        var totalSize = 0L

        for (node in nodes) {
            totalSize += if (node.isFolder) nodesTotalSize(megaApi.getChildren(node)) else node.size
        }

        return totalSize
    }

    private fun notEnoughSpace(parentPath: String, totalSize: Long): Boolean {
        var availableFreeSpace = Long.MAX_VALUE
        try {
            val stat = StatFs(parentPath)
            availableFreeSpace = stat.availableBlocksLong * stat.blockSizeLong
        } catch (ex: Exception) {
        }
        logDebug("availableFreeSpace: $availableFreeSpace, totalSize: $totalSize")

        if (availableFreeSpace < totalSize) {
            post { snackbarShower.showNotEnoughSpaceSnackbar() }
            logWarning("Not enough space")
            return true
        }

        return false
    }

    private fun checkSizeBeforeDownload(parentPath: String) {
        if (notEnoughSpace(parentPath, saving.totalSize())) {
            return
        }

        if (TextUtils.equals(dbHandler.attributes.askSizeDownload, false.toString())
            || saving.totalSize() < CONFIRM_SIZE_MIN_BYTES
        ) {
            checkInstalledAppBeforeDownload(parentPath)
            return
        }

        showConfirmationDialog(
            getString(R.string.alert_larger_file, getSizeString(saving.totalSize()))
        ) { notShowAgain ->
            if (notShowAgain) {
                Completable.fromCallable { dbHandler.setAttrAskSizeDownload(false.toString()) }
                    .subscribeOn(Schedulers.io())
                    .subscribeBy(onError = { logErr("NodeSaver checkSizeBeforeDownload") })
                    .addTo(compositeDisposable)
            }

            checkInstalledAppBeforeDownload(parentPath)
        }
    }

    private fun checkInstalledAppBeforeDownload(parentPath: String) {
        if (TextUtils.equals(dbHandler.attributes.askNoAppDownload, false.toString())) {
            download(parentPath)
            return
        }

        if (!saving.hasUnsupportedFile(app)) {
            download(parentPath)
            return
        }

        showConfirmationDialog(
            getString(R.string.alert_no_app, saving.unsupportedFileName)
        ) { notShowAgain ->
            if (notShowAgain) {
                Completable.fromCallable { dbHandler.setAttrAskNoAppDownload(false.toString()) }
                    .subscribeOn(Schedulers.io())
                    .subscribeBy(onError = { logErr("NodeSaver checkInstalledAppBeforeDownload") })
                    .addTo(compositeDisposable)
            }

            download(parentPath)
        }
    }

    private fun download(parentPath: String) {
        Completable
            .fromCallable {
                checkParentPathAndDownload(parentPath)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = { logErr("NodeSaver download") })
            .addTo(compositeDisposable)
    }

    private fun checkParentPathAndDownload(parentPath: String) {
        if (MegaApplication.getInstance().storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning()
            return
        }

        val sdCardOperator = SDCardOperator.initSDCardOperator(app, parentPath)
        if (sdCardOperator == null) {
            requestLocalFolder(
                getString(R.string.no_external_SD_card_detected),
                activityLauncher
            )
            return
        }

        val autoPlayInfo = saving.doDownload(
            megaApi, megaApiFolder, parentPath, SDCardOperator.isSDCardPath(parentPath),
            sdCardOperator, snackbarShower
        )

        if (!autoPlayInfo.couldAutoPlay || dbHandler.autoPlayEnabled != true.toString()) {
            return
        }

        if (saving.fromMediaViewer()) {
            snackbarShower.showSnackbar(getString(R.string.general_already_downloaded))
        } else {
            autoPlayNode(app, autoPlayInfo, activityLauncher, snackbarShower)
        }
    }

    private fun showConfirmationDialog(message: String, onConfirmed: (Boolean) -> Unit) {
        post { confirmDialogShower(message, onConfirmed) }
    }

    private fun hasWriteExternalStoragePermission(): Boolean =
        hasPermissions(app, permission.WRITE_EXTERNAL_STORAGE)

    private fun lackPermission(): Boolean {
        if (!hasWriteExternalStoragePermission()) {
            permissionRequester.askPermissions(
                arrayOf(permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE
            )
            return true
        }
        return false
    }

    companion object {
        const val CONFIRM_SIZE_MIN_BYTES = 100 * 1024 * 1024L

        private const val STATE_KEY_SAVING = "saving"
    }
}
