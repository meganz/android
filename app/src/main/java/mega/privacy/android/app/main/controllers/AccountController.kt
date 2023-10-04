package mega.privacy.android.app.main.controllers

import android.Manifest
import android.content.Context
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil.saveTextOnFile
import mega.privacy.android.app.utils.StorageUtils.thereIsNotEnoughFreeSpace
import mega.privacy.android.app.utils.Util.isOffline
import mega.privacy.android.app.utils.Util.showSnackbar
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class AccountController @Inject constructor(
    @ActivityContext private val context: Context,
) {

    /**
     * Export recovery key file to a selected location on file system.
     *
     * @param path The selected location.
     */
    fun exportMK(path: String?) {
        Timber.d("exportMK")

        if (isOffline(context)) {
            return
        }

        val megaApi = MegaApplication.getInstance().megaApi
        val key = megaApi.exportMasterKey()

        if (context is ManagerActivity) {
            megaApi.masterKeyExported(context)
        } else if (context is TestPasswordActivity) {
            megaApi.masterKeyExported(context)
        }

        if (!hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (context is ManagerActivity) {
                requestPermission(
                    context,
                    Constants.REQUEST_WRITE_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } else if (context is TestPasswordActivity) {
                requestPermission(
                    context,
                    Constants.REQUEST_WRITE_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }

            return
        }

        if (thereIsNotEnoughFreeSpace(path!!)) {
            showSnackbar(context, context.getString(R.string.error_not_enough_free_space))
            return
        }

        if (saveTextOnFile(context, key, path)) {
            showSnackbar(context, context.getString(R.string.save_MK_confirmation))

            if (context is TestPasswordActivity) {
                context.onRecoveryKeyExported()
            }
        }
    }

}
