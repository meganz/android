package mega.privacy.android.app.components.saver

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.text.TextUtils
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.*
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode.PICK_FOLDER
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_TREE
import mega.privacy.android.app.utils.FileUtil.getDownloadLocation
import mega.privacy.android.app.utils.FileUtil.getFullPathFromTreeUri
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaApiJava
import java.util.concurrent.Callable

/**
 * A class that encapsulate all the procedure of saving a node into device,
 * including choose save to internal storage or external sdcard,
 * choose save path, check download size, check other apps that could open this file, etc,
 * the final step that really download the node into a file is handled in sub-classes,
 * by implementing the abstract doDownload function.
 *
 * The initiation API of save should also be added by sub-classes, because it's usually
 * related with the final download step.
 *
 * It simplifies code in activity/fragment where a node need to be saved.
 */
abstract class NodeSaver(
    protected val context: Context,
    protected val dbHandler: DatabaseHandler,
) {
    private val compositeDisposable = CompositeDisposable()
    private val uiHandler = Handler(Looper.getMainLooper())
    protected var saving = Saving.NOTHING
    private var activityStarter: (Intent, Int) -> Unit = { _, _ -> }

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
                if (resultCode != Activity.RESULT_OK) {
                    showSnackbar(context, context.getString(R.string.download_requires_permission))
                } else {
                    showSnackbar(context, context.getString(R.string.no_external_SD_card_detected))
                }
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
     * Initiate the saving.
     *
     * @param activityStarter a high-order function to launch activity when needed
     * @param savingProducer a high-order function to produce internal state needed for later use
     */
    protected fun save(activityStarter: (Intent, Int) -> Unit, savingProducer: () -> Saving?) {
        if (lackPermission()) {
            return
        }

        add(
            Completable
                .fromCallable(Callable {
                    val saving = savingProducer() ?: return@Callable
                    this.saving = saving
                    this.activityStarter = activityStarter
                    val downloadLocationDefaultPath = getDownloadLocation()

                    if (askMe(context)) {
                        requestLocalFolder(null, activityStarter)
                    } else {
                        checkSizeBeforeDownload(downloadLocationDefaultPath)
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("NodeSaver save"))
        )
    }

    private fun requestLocalFolder(
        prompt: String?, activityStarter: (Intent, Int) -> Unit
    ) {
        val intent = Intent(PICK_FOLDER.action)
        intent.putExtra(EXTRA_BUTTON_PREFIX, context.getString(R.string.general_select))
        intent.putExtra(EXTRA_FROM_SETTINGS, false)
        intent.setClass(context, FileStorageActivityLollipop::class.java)

        if (prompt != null) {
            intent.putExtra(EXTRA_PROMPT, prompt)
        }

        activityStarter(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER)
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
            runOnUiThread { showNotEnoughSpaceSnackbar(context) }
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
            context.getString(R.string.alert_larger_file, getSizeString(saving.totalSize))
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
                activityStarter
            )
            return
        }

        doDownload(parentPath, SDCardOperator.isSDCardPath(parentPath), sdCardOperator)
    }

    /**
     * The final step to download a node into a file.
     *
     * @param parentPath the parent path where the file should be inside
     * @param externalSDCard whether it's download into external sdcard
     * @param sdCardOperator SDCardOperator used when download to external sdcard,
     * will be null if download to internal storage
     */
    abstract fun doDownload(
        parentPath: String,
        externalSDCard: Boolean,
        sdCardOperator: SDCardOperator?
    )

    private fun showConfirmationDialog(message: String, onConfirmed: (Boolean) -> Unit) {
        runOnUiThread { doShowConfirmationDialog(message, onConfirmed) }
    }

    private fun doShowConfirmationDialog(message: String, onConfirmed: (Boolean) -> Unit) {
        val confirmationLayout = LinearLayout(context)
        confirmationLayout.orientation = LinearLayout.VERTICAL
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(
            scaleWidthPx(20, context.resources.displayMetrics),
            scaleHeightPx(10, context.resources.displayMetrics),
            scaleWidthPx(17, context.resources.displayMetrics),
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

    private fun lackPermission(): Boolean {
        if (VERSION.SDK_INT >= VERSION_CODES.M && ContextCompat.checkSelfPermission(
                context, permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            NodeController(context).askForPermissions()
            return true
        }
        return false
    }

    protected fun add(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    /**
     * Clear all internal state and cancel all flying operation, should be called
     * in onDestroy lifecycle callback.
     */
    fun destroy() {
        compositeDisposable.dispose()
    }

    protected fun runOnUiThread(task: () -> Unit) {
        uiHandler.post(task)
    }

    private fun runOnUiThreadDelayed(delayMs: Long, task: () -> Unit) {
        uiHandler.postDelayed(task, delayMs)
    }

    companion object {
        const val CONFIRM_SIZE_MIN_BYTES = 100 * 1024 * 1024L
    }
}
