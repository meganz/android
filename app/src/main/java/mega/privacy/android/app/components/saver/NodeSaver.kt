package mega.privacy.android.app.components.saver

import android.Manifest.permission
import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog.Builder
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.EXTRA_FROM_SETTINGS
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.EXTRA_PATH
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.EXTRA_PROMPT
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.EXTRA_SD_ROOT
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode.PICK_FOLDER
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_TREE
import mega.privacy.android.app.utils.FileUtil.getFullPathFromTreeUri
import mega.privacy.android.app.utils.FileUtils.getDownloadLocation
import mega.privacy.android.app.utils.FileUtils.isBasedOnFileStorage
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.SDCardOperator.SDCardException
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.askMe
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.app.utils.Util.scaleHeightPx
import mega.privacy.android.app.utils.Util.scaleWidthPx
import mega.privacy.android.app.utils.Util.showNotEnoughSpaceSnackbar
import java.io.File

abstract class NodeSaver(
    protected val context: Context,
    private val dbHandler: DatabaseHandler
) {
    private val compositeDisposable = CompositeDisposable()
    private val uiHandler = Handler(Looper.getMainLooper())
    protected var saving = Saving.NOTHING
    private var activityStarter: (Intent, Int) -> Unit = { _, _ -> }

    fun handleActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == Activity.RESULT_OK) {
            logDebug("REQUEST_CODE_SELECT_LOCAL_FOLDER")
            if (intent == null) {
                logWarning("Intent null")
                return true
            }

            val parentPath = intent.getStringExtra(EXTRA_PATH)
            if (parentPath == null) {
                logWarning("parentPath null")
                return true
            }

            add(Completable.fromCallable { checkSizeBeforeDownload(parentPath) }
                .subscribeOn(Schedulers.io())
                .subscribe(IGNORE, logErr("NodeSaver handleActivityResult")))
        } else if (requestCode == REQUEST_CODE_TREE) {
            if (intent == null) {
                logWarning("handleActivityResult REQUEST_CODE_TREE: result intent is null")
                if (resultCode != Activity.RESULT_OK) {
                    Util.showSnackbar(
                        context, context.getString(R.string.download_requires_permission)
                    )
                } else {
                    Util.showSnackbar(
                        context, context.getString(R.string.no_external_SD_card_detected)
                    )
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
        }

        return false
    }

    protected fun save(activityStarter: (Intent, Int) -> Unit, savingProducer: () -> Saving) {
        if (lackPermission()) {
            return
        }

        add(Completable
            .fromCallable {
                saving = savingProducer()
                this.activityStarter = activityStarter
                val downloadLocationDefaultPath = getDownloadLocation()
                if (askMe(context)) {
                    logDebug("askMe")
                    val fs = context.getExternalFilesDirs(null)
                    if (fs.size <= 1 || fs[1] == null) {
                        requestLocalFolder(null, null, activityStarter)
                    } else {
                        runOnUiThread { showSelectDownloadLocationDialog(activityStarter) }
                    }
                } else {
                    logDebug("NOT askMe")
                    File(downloadLocationDefaultPath).mkdirs()
                    checkSizeBeforeDownload(downloadLocationDefaultPath)
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribe(IGNORE, logErr("NodeSaver save")))
    }

    private fun showSelectDownloadLocationDialog(activityStarter: (Intent, Int) -> Unit) {
        Builder(context)
            .setTitle(R.string.title_select_download_location)
            .setNegativeButton(R.string.general_cancel) { dialog, _ -> dialog.cancel() }
            .setItems(R.array.settings_storage_download_location_array) { _, which ->
                when (which) {
                    0 -> requestLocalFolder(null, null, activityStarter)
                    1 -> handleSdCard(activityStarter)
                }
            }
            .create()
            .show()
    }

    private fun handleSdCard(activityStarter: (Intent, Int) -> Unit) {
        try {
            val sdCardOperator = SDCardOperator(context)
            val sdCardRoot = sdCardOperator.sdCardRoot
            if (sdCardOperator.canWriteWithFile(sdCardRoot)) {
                requestLocalFolder(sdCardRoot, null, activityStarter)
            } else {
                if (isBasedOnFileStorage()) {
                    try {
                        sdCardOperator.initDocumentFileRoot(dbHandler.sdCardUri)
                        requestLocalFolder(sdCardRoot, null, activityStarter)
                    } catch (e: SDCardException) {
                        logError(
                            "SDCardOperator initDocumentFileRoot failed, requestSDCardPermission", e
                        )
                        requestSDCardPermission(sdCardRoot, activityStarter)
                    }
                } else {
                    requestSDCardPermission(sdCardRoot, activityStarter)
                }
            }
        } catch (e: SDCardException) {
            logError("Initialize SDCardOperator failed", e)
            // sd card is available, choose internal storage location
            requestLocalFolder(null, null, activityStarter)
        }
    }

    private fun requestSDCardPermission(
        sdCardRoot: String,
        activityStarter: (Intent, Int) -> Unit
    ) {
        val intent = SDCardOperator.getRequestPermissionIntent(context, sdCardRoot)
        activityStarter(intent, REQUEST_CODE_TREE)
    }

    private fun requestLocalFolder(
        sdRoot: String?, prompt: String?, activityStarter: (Intent, Int) -> Unit
    ) {
        val intent = Intent(PICK_FOLDER.action)
        intent.putExtra(EXTRA_BUTTON_PREFIX, context.getString(R.string.general_select))
        intent.putExtra(EXTRA_FROM_SETTINGS, false)
        intent.setClass(context, FileStorageActivityLollipop::class.java)
        if (sdRoot != null) {
            intent.putExtra(EXTRA_SD_ROOT, sdRoot)
        }
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

    fun download(parentPath: String) {
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
        val externalSDCard = SDCardOperator.isSDCardPath(parentPath)
        var sdCardOperator: SDCardOperator? = null
        if (externalSDCard) {
            try {
                sdCardOperator = SDCardOperator(context)
            } catch (e: SDCardException) {
                logError("Initialize SDCardOperator failed", e)
                requestLocalFolder(
                    null, context.getString(R.string.no_external_SD_card_detected), activityStarter
                )
            }
        }
        if (externalSDCard && sdCardOperator == null) {
            return
        }

        if (sdCardOperator != null && sdCardOperator.isNewSDCardPath(parentPath)) {
            logDebug("new sd card, check permission.")
            runOnUiThread {
                Toast.makeText(
                    context, R.string.old_sdcard_unavailable, Toast.LENGTH_LONG
                ).show()
            }
            runOnUiThreadDelayed(1500) {
                showSelectDownloadLocationDialog(activityStarter)
            }
            return
        }

        if (sdCardOperator != null && !sdCardOperator.canWriteWithFile(parentPath)) {
            try {
                sdCardOperator.initDocumentFileRoot(dbHandler.sdCardUri);
            } catch (e: SDCardException) {
                logError("SDCardOperator initDocumentFileRoot failed requestSDCardPermission", e)
                requestSDCardPermission(sdCardOperator.sdCardRoot, activityStarter)
                return
            }
        }

        doDownload(parentPath, externalSDCard, sdCardOperator)
    }

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

        AlertDialog.Builder(context)
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

    abstract class Saving(
        val totalSize: Long,
        val highPriority: Boolean
    ) {
        var unsupportedFileName = ""
            protected set

        abstract fun hasUnsupportedFile(context: Context): Boolean

        companion object {
            val NOTHING = object : Saving(0, false) {
                override fun hasUnsupportedFile(context: Context): Boolean = false
            }
        }
    }
}
