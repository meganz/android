package mega.privacy.android.app.exportRK

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.exportRK.ExportRecoveryKeyActivity.Companion.WRITE_STORAGE_TO_SAVE_RK
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.utils.Constants.REQUEST_DOWNLOAD_FOLDER
import mega.privacy.android.app.utils.FileUtil.*
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.app.utils.TextUtil.copyToClipboard
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

@HiltViewModel
class ExportRecoveryKeyViewModel @Inject constructor(
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
    fun copyRK(activity: Activity, action: (String?) -> Unit) {
        val textRK = exportRK()

        if (!isTextEmpty(textRK)) {
            copyToClipboard(activity, textRK)
        }

        action.invoke(textRK)
    }

    /**
     * Exports the Recovery Key
     */
    private fun exportRK(): String? {
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
     * @param activity    Current activity.
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */
    fun manageActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_DOWNLOAD_FOLDER || resultCode != RESULT_OK || data == null) {
            logWarning("Wrong activity result.")
            return
        }

        val exportedRK = exportRK()

        saveRKAction?.invoke(
            when {
                exportedRK.isNullOrEmpty() -> GENERAL_ERROR
                saveTextOnContentUri(activity.contentResolver, data.data, exportedRK) -> RK_EXPORTED
                else -> GENERAL_ERROR
            }
        )
    }
}