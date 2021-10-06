package mega.privacy.android.app.exportRK

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.exportRK.ExportRecoveryKeyActivity.Companion.WRITE_STORAGE_TO_SAVE_RK
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.getRecoveryKeyFileName
import mega.privacy.android.app.utils.FileUtil.saveTextOnFile
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.app.utils.StorageUtils.thereIsNotEnoughFreeSpace
import mega.privacy.android.app.utils.TextUtil.copyToClipboard
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.isOffline
import nz.mega.sdk.MegaApiAndroid
import java.io.File

class ExportRecoveryKeyViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    companion object {
        const val ERROR_NO_SPACE = "ERROR_NO_SPACE"
        const val GENERAL_ERROR = "GENERAL_ERROR"
        const val RK_EXPORTED = "RK_EXPORTED"
    }

    private var saveRKAction: ((String) -> Unit)? = null

    /**
     * Copies the Recovery Key to the clipboard.
     *
     * @param activity Current activity.
     * @param action   Action to perform after copy finishes.
     */
    fun copyRK(activity: Activity, action: (String) -> Unit) {
        val textRK = exportRK()

        if (!isTextEmpty(textRK)) {
            copyToClipboard(activity, textRK)
        }

        action.invoke(textRK)
    }

    /**
     * Exports the Recovery Key
     */
    private fun exportRK(): String {
        val textRK = megaApi.exportMasterKey()

        if (!isTextEmpty(textRK)) {
            megaApi.masterKeyExported(null)
        }

        return textRK
    }

    /**
     * Before export the Recovery Key, checks if the app has granted the necessary permissions.
     *
     * @param activity     Current activity.
     * @param saveRKAction Action to perform after the export action finishes.
     */
    fun checkPermissionsBeforeSaveRK(activity: Activity, saveRKAction: (String) -> Unit) {
        this.saveRKAction = saveRKAction

        if (hasPermissions(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRK(activity)
        } else {
            requestPermission(
                activity,
                WRITE_STORAGE_TO_SAVE_RK,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Saves the Recovery Key into File System.
     *
     * @param activity Current activity.
     */
    fun saveRK(activity: Activity) {
        AccountController.saveRkToFileSystem(activity)
    }

    /**
     * Manages onActivityResult.
     *
     * @param context     Current context.
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */
    fun manageActivityResult(context: Context, requestCode: Int, resultCode: Int, data: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && requestCode == REQUEST_WRITE_STORAGE && Environment.isExternalStorageManager()) {
            saveRK(context as ExportRecoveryKeyActivity)
        } else if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK && data != null) {
            val parentPath: String =
                data.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH) ?: return
            val path = parentPath + File.separator + getRecoveryKeyFileName()
            val sdCardUriString = data.getStringExtra(FileStorageActivityLollipop.EXTRA_SD_URI)
            saveRKOnChosenPath(context, path, sdCardUriString)
        } else {
            logWarning("Wrong activity result.")
        }
    }

    /**
     * Saves the Recovery Key on chosen path.
     *
     * @param context         Current context.
     * @param path            The selected location to save the file.
     * @param sdCardUriString If the selected location is on SD card,
     *                        need the uri to grant SD card write permission.
     */
    private fun saveRKOnChosenPath(context: Context, path: String, sdCardUriString: String?) {
        if (isOffline(context)) {
            return
        }

        if (thereIsNotEnoughFreeSpace(path)) {
            saveRKAction?.invoke(ERROR_NO_SPACE)
            return
        }

        val textRK = exportRK()

        if (isTextEmpty(textRK)) {
            saveRKAction?.invoke(GENERAL_ERROR)
            return
        }

        saveRKAction?.invoke(
            if (saveTextOnFile(context, textRK, path, sdCardUriString)) RK_EXPORTED
            else GENERAL_ERROR
        )
    }
}