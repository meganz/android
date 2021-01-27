package mega.privacy.android.app.components.saver

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.StatFs
import android.text.TextUtils
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.qualifiers.ActivityContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.interfaces.*
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.*
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode.PICK_FOLDER
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.getDownloadLocation
import mega.privacy.android.app.utils.FileUtil.getFullPathFromTreeUri
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import java.util.concurrent.Callable
import javax.inject.Inject

/**
 * A class that encapsulate all the procedure of saving nodes into device,
 * including choose save to internal storage or external sdcard,
 * choose save path, check download size, check other apps that could open this file, etc,
 * the final step that really download the node into a file is handled in sub-classes,
 * by implementing the abstract doDownload function.
 *
 * The initiation API of save should also be added by sub-classes, because it's usually
 * related with the final download step.
 *
 * It simplifies code in activity/fragment where nodes need to be saved.
 */
class NodeSaver @Inject constructor(
    @ActivityContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
) {
    private val compositeDisposable = CompositeDisposable()

    private var saving = Saving.NOTHING
    private var activityLauncher = ActivityLauncher.IDLE
    private var permissionRequester = PermissionRequester.IDLE
    private var snackbarShower = SnackbarShower.IDLE

    /**
     * Save an offline node into device.
     *
     * @param node the offline node to save
     * @param activityLauncher interface to start activity
     * @param permissionRequester interface to request permission
     * @param snackbarShower interface to show snackbar
     */
    fun saveOfflineNode(
        node: MegaOffline,
        activityLauncher: ActivityLauncher,
        permissionRequester: PermissionRequester,
        snackbarShower: SnackbarShower
    ) {
        saveOfflineNodes(listOf(node), activityLauncher, permissionRequester, snackbarShower)
    }

    /**
     * Save offline nodes into device.
     *
     * @param nodes the offline nodes to save
     * @param activityLauncher interface to start activity
     * @param permissionRequester interface to request permission
     * @param snackbarShower interface to show snackbar
     */
    fun saveOfflineNodes(
        nodes: List<MegaOffline>,
        activityLauncher: ActivityLauncher,
        permissionRequester: PermissionRequester,
        snackbarShower: SnackbarShower
    ) {
        this.activityLauncher = activityLauncher
        this.permissionRequester = permissionRequester
        this.snackbarShower = snackbarShower

        save {
            var totalSize = 0L
            for (node in nodes) {
                totalSize += FileUtil.getTotalSize(OfflineUtils.getOfflineFile(context, node))
            }
            OfflineSaving(totalSize, nodes)
        }
    }

    /**
     * Save a MegaNode into device.
     *
     * @param handle the handle of node to save
     * @param activityLauncher interface to start activity
     * @param permissionRequester interface to request permission
     * @param snackbarShower interface to show snackbar
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param fromChat whether this download is from chat
     */
    fun saveHandle(
        handle: Long,
        activityLauncher: ActivityLauncher,
        permissionRequester: PermissionRequester,
        snackbarShower: SnackbarShower,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        fromChat: Boolean = false
    ) {
        saveHandles(
            listOf(handle), activityLauncher, permissionRequester, snackbarShower,
            highPriority, isFolderLink, fromMediaViewer, fromChat
        )
    }

    /**
     * Save a list of MegaNode into device.
     *
     * @param handles the handle list of nodes to save
     * @param activityLauncher interface to start activity
     * @param permissionRequester interface to request permission
     * @param snackbarShower interface to show snackbar
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param fromChat whether this download is from chat
     */
    fun saveHandles(
        handles: List<Long>,
        activityLauncher: ActivityLauncher,
        permissionRequester: PermissionRequester,
        snackbarShower: SnackbarShower,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        fromChat: Boolean = false
    ) {
        this.activityLauncher = activityLauncher
        this.permissionRequester = permissionRequester
        this.snackbarShower = snackbarShower

        save {
            val nodes = ArrayList<MegaNode>()
            var totalSize = 0L

            for (handle in handles) {
                val node = megaApi.getNodeByHandle(handle)
                if (node != null) {
                    nodes.add(node)
                    totalSize += node.size
                }
            }

            MegaNodeSaving(totalSize, highPriority, isFolderLink, nodes, fromMediaViewer, fromChat)
        }
    }

    /**
     * Save a MegaNode into device.
     *
     * @param node node to save
     * @param activityLauncher interface to start activity
     * @param permissionRequester interface to request permission
     * @param snackbarShower interface to show snackbar
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param fromChat whether this download is from chat
     */
    fun saveNode(
        node: MegaNode,
        activityLauncher: ActivityLauncher,
        permissionRequester: PermissionRequester,
        snackbarShower: SnackbarShower,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        fromChat: Boolean = false
    ) {
        saveNodes(
            listOf(node), activityLauncher, permissionRequester, snackbarShower,
            highPriority, isFolderLink, fromMediaViewer, fromChat
        )
    }

    /**
     * Save a list of MegaNode into device.
     *
     * @param nodes nodes to save
     * @param activityLauncher interface to start activity
     * @param permissionRequester interface to request permission
     * @param snackbarShower interface to show snackbar
     * @param highPriority whether this download is high priority or not
     * @param isFolderLink whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param fromChat whether this download is from chat
     */
    fun saveNodes(
        nodes: List<MegaNode>,
        activityLauncher: ActivityLauncher,
        permissionRequester: PermissionRequester,
        snackbarShower: SnackbarShower,
        highPriority: Boolean = false,
        isFolderLink: Boolean = false,
        fromMediaViewer: Boolean = false,
        fromChat: Boolean = false
    ) {
        this.activityLauncher = activityLauncher
        this.permissionRequester = permissionRequester
        this.snackbarShower = snackbarShower

        save {
            var totalSize = 0L

            for (node in nodes) {
                totalSize += node.size
            }

            MegaNodeSaving(totalSize, highPriority, isFolderLink, nodes, fromMediaViewer, fromChat)
        }
    }

    /**
     * Handle activity result from FileStorageActivityLollipop launched by requestLocalFolder,
     * and take actions according to the state and result.
     *
     * It should be called in onActivityResult (but this doesn't mean NodeSaver should be
     * owned by a fragment or activity).
     *
     * @param requestCode the requestCode from onActivityResult
     * @param resultCode the resultCode from onActivityResult
     * @param intent the intent from onActivityResult
     * @return whether NodeSaver handles this result, if this method return false,
     * fragment/activity should handle the result by other code.
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        if (saving == Saving.NOTHING) {
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

            add(Completable.fromCallable { checkSizeBeforeDownload(parentPath) }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("NodeSaver handleActivityResult")))

            return true
        } else if (requestCode == REQUEST_CODE_TREE) {
            if (intent == null) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: result intent is null")

                val message = if (resultCode != Activity.RESULT_OK) {
                    context.getString(R.string.download_requires_permission)
                } else {
                    context.getString(R.string.no_external_SD_card_detected)
                }

                snackbarShower.showSnackbar(message)

                return false
            }

            val uri = intent.data
            if (uri == null) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: tree uri is null!")
                return false
            }

            val pickedDir = DocumentFile.fromTreeUri(context, uri)
            if (pickedDir == null || !pickedDir.canWrite()) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: pickedDir not writable")
                return false
            }

            dbHandler.sdCardUri = uri.toString()

            val parentPath = getFullPathFromTreeUri(uri, context)
            if (parentPath == null) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: parentPath is null")
                return false
            }

            add(Completable.fromCallable { checkSizeBeforeDownload(parentPath) }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("NodeSaver handleActivityResult")))

            return true
        }

        return false
    }

    /**
     * Handle request permission result, and take actions according to the state and result.
     *
     * It should be called in onRequestPermissionsResult (but this doesn't mean NodeSaver should be
     * owned by an activity).
     *
     * @param requestCode the requestCode from onRequestPermissionsResult
     * @return whether NodeSaver handles this result, if this method return false,
     * activity should handle the result by other code.
     */
    fun handleRequestPermissionsResult(requestCode: Int): Boolean {
        if (requestCode != REQUEST_WRITE_STORAGE) {
            return false;
        }

        if (hasWriteExternalStoragePermission()) {
            add(Completable
                .fromCallable { doSave() }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("NodeSaver save")))
        }

        return true
    }

    /**
     * Initiate the saving.
     *
     * @param savingProducer a high-order function to produce internal state needed for later use
     */
    private fun save(savingProducer: () -> Saving?) {
        add(
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
                .subscribe(IGNORE, logErr("NodeSaver save"))
        )
    }

    private fun doSave() {
        val downloadLocationDefaultPath = getDownloadLocation()

        if (Util.askMe(context)) {
            requestLocalFolder(null, activityLauncher)
        } else {
            checkSizeBeforeDownload(downloadLocationDefaultPath)
        }
    }

    private fun requestLocalFolder(
        prompt: String?, activityLauncher: ActivityLauncher
    ) {
        val intent = Intent(PICK_FOLDER.action)
        intent.putExtra(EXTRA_BUTTON_PREFIX, context.getString(R.string.general_select))
        intent.putExtra(EXTRA_FROM_SETTINGS, false)
        intent.setClass(context, FileStorageActivityLollipop::class.java)

        if (prompt != null) {
            intent.putExtra(EXTRA_PROMPT, prompt)
        }

        activityLauncher.launchActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER)
    }

    private fun checkSizeBeforeDownload(parentPath: String) {
        var availableFreeSpace = Long.MAX_VALUE
        try {
            val stat = StatFs(parentPath)
            availableFreeSpace = stat.availableBlocksLong * stat.blockSizeLong
        } catch (ex: Exception) {
        }
        logDebug("availableFreeSpace: $availableFreeSpace, totalSize: ${saving.totalSize}")

        if (availableFreeSpace < saving.totalSize) {
            post { snackbarShower.showNotEnoughSpaceSnackbar() }
            logWarning("Not enough space")
            return
        }

        if (TextUtils.equals(dbHandler.attributes.askSizeDownload, false.toString())
            || saving.totalSize < CONFIRM_SIZE_MIN_BYTES
        ) {
            checkInstalledAppBeforeDownload(parentPath)
            return
        }

        showConfirmationDialog(
            context.getString(R.string.alert_larger_file, Util.getSizeString(saving.totalSize))
        ) { notShowAgain ->
            if (notShowAgain) {
                add(Completable.fromCallable { dbHandler.setAttrAskSizeDownload(false.toString()) }
                    .subscribeOn(Schedulers.io())
                    .subscribe(IGNORE, logErr("NodeSaver setAttrAskSizeDownload")))
            }

            checkInstalledAppBeforeDownload(parentPath)
        }
    }

    private fun checkInstalledAppBeforeDownload(parentPath: String) {
        if (TextUtils.equals(dbHandler.attributes.askNoAppDownload, false.toString())) {
            download(parentPath)
            return
        }

        if (!saving.hasUnsupportedFile(context)) {
            download(parentPath)
            return
        }

        showConfirmationDialog(
            context.getString(R.string.alert_no_app, saving.unsupportedFileName)
        ) { notShowAgain ->
            if (notShowAgain) {
                add(Completable.fromCallable { dbHandler.setAttrAskNoAppDownload(false.toString()) }
                    .subscribeOn(Schedulers.io())
                    .subscribe(IGNORE, logErr("NodeSaver setAttrAskNoAppDownload")))
            }

            download(parentPath)
        }
    }

    private fun download(parentPath: String) {
        add(Completable
            .fromCallable {
                checkParentPathAndDownload(parentPath)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(IGNORE, logErr("NodeSaver download"))
        )
    }

    private fun checkParentPathAndDownload(parentPath: String) {
        if (MegaApplication.getInstance().storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning()
            return
        }

        val sdCardOperator = SDCardOperator.initSDCardOperator(context, parentPath)
        if (sdCardOperator == null) {
            requestLocalFolder(
                context.getString(R.string.no_external_SD_card_detected),
                activityLauncher
            )
            return
        }

        saving.doDownload(
            parentPath,
            SDCardOperator.isSDCardPath(parentPath),
            sdCardOperator,
            snackbarShower
        )
    }

    private fun showConfirmationDialog(message: String, onConfirmed: (Boolean) -> Unit) {
        post { doShowConfirmationDialog(message, onConfirmed) }
    }

    private fun doShowConfirmationDialog(message: String, onConfirmed: (Boolean) -> Unit) {
        val confirmationLayout = LinearLayout(context)
        confirmationLayout.orientation = LinearLayout.VERTICAL
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(
            Util.scaleWidthPx(20, context.resources.displayMetrics),
            Util.scaleHeightPx(10, context.resources.displayMetrics),
            Util.scaleWidthPx(17, context.resources.displayMetrics),
            0
        )

        val notShowAgain = CheckBox(context)
        notShowAgain.setText(R.string.checkbox_not_show_again)
        notShowAgain.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        confirmationLayout.addView(notShowAgain, params)

        MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialogStyle)
            .setView(confirmationLayout)
            .setMessage(message)
            .setPositiveButton(
                R.string.general_save_to_device
            ) { _, _ ->
                onConfirmed(notShowAgain.isChecked)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
            .show()
    }

    private fun hasWriteExternalStoragePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED

    private fun lackPermission(): Boolean {
        if (!hasWriteExternalStoragePermission()) {
            permissionRequester.askPermissions(
                arrayOf(permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE
            )
            return true
        }
        return false
    }

    private fun add(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    /**
     * Clear all internal state and cancel all flying operation, should be called
     * in onDestroy lifecycle callback.
     */
    fun destroy() {
        compositeDisposable.dispose()
    }

    companion object {
        const val CONFIRM_SIZE_MIN_BYTES = 100 * 1024 * 1024L
    }
}
