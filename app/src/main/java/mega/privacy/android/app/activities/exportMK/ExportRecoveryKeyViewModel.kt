package mega.privacy.android.app.activities.exportMK

import android.Manifest
import android.app.Activity
import android.content.Context
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.PermissionUtils.requestPermission

class ExportRecoveryKeyViewModel : BaseRxViewModel() {

    companion object {
        private const val WRITE_STORAGE_TO_SAVE_MK = 1
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
        context: Context,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty()) {
            LogUtil.logWarning("Permissions ${permissions[0]} not granted")
            return
        }

        if (requestCode == WRITE_STORAGE_TO_SAVE_MK
            && hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            saveMK(context)
        }
    }

    private fun saveMK(context: Context) {
        AccountController(context).saveRkToFileSystem()
    }
}