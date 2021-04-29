package mega.privacy.android.app.activities.exportMK

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.utils.Constants.REQUEST_DOWNLOAD_FOLDER
import mega.privacy.android.app.utils.FileUtil.getRecoveryKeyFileName
import mega.privacy.android.app.utils.FileUtil.saveTextOnFile
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.PermissionUtils.requestPermission
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
        private const val WRITE_STORAGE_TO_SAVE_MK = 1
        const val ERROR_NO_SPACE = "ERROR_NO_SPACE"
        const val GENERAL_ERROR = "GENERAL_ERROR"
        const val RK_EXPORTED = "RK_EXPORTED"
    }

    private val copiedMK: MutableLiveData<String> = MutableLiveData()
    private val exportedMK: MutableLiveData<String> = MutableLiveData()

    fun onMKCopied(): LiveData<String> = copiedMK

    fun onMKExported(): LiveData<String> = exportedMK

    fun copyMK(context: Context) {
        val textMK = exportMK()

        if (!isTextEmpty(textMK)) {
            copyToClipboard(context, textMK)
        }

        copiedMK.value = textMK
    }

    private fun exportMK(): String {
        val textMK = megaApi.exportMasterKey()

        if (!isTextEmpty(textMK)) {
            megaApi.masterKeyExported(null)
        }

        return textMK
    }

    fun checkPermissionsBeforeSaveMK(activity: Activity) {
        if (hasPermissions(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveMK(activity)
        } else {
            requestPermission(
                activity,
                WRITE_STORAGE_TO_SAVE_MK,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    fun managePermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (grantResults.isEmpty() || requestCode != WRITE_STORAGE_TO_SAVE_MK) {
            logWarning("Permissions ${permissions[0]} not granted")
        }

        return if (hasPermissions(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveMK(activity)
            true
        } else {
            false
        }
    }

    private fun saveMK(activity: Activity) {
        AccountController.saveRkToFileSystem(activity)
    }

    fun manageActivityResult(context: Context, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_DOWNLOAD_FOLDER || resultCode != RESULT_OK || data == null) {
            logWarning("Wrong activity result.")
            return
        }

        val parentPath: String =
            data.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH) ?: return
        val path = parentPath + File.separator + getRecoveryKeyFileName()
        val sdCardUriString = data.getStringExtra(FileStorageActivityLollipop.EXTRA_SD_URI)
        saveMKOnChosenPath(context, path, sdCardUriString)
    }

    private fun saveMKOnChosenPath(context: Context, path: String, sdCardUriString: String?) {
        if (isOffline(context)) {
            return
        }

        if (thereIsNotEnoughFreeSpace(path)) {
            exportedMK.value = ERROR_NO_SPACE
            return
        }

        val textMK = exportMK()

        if (isTextEmpty(textMK)) {
            exportedMK.value = GENERAL_ERROR
        }

        exportedMK.value =
            if (saveTextOnFile(context, textMK, path, sdCardUriString)) RK_EXPORTED
            else GENERAL_ERROR
    }
}