package mega.privacy.android.app.utils.permission

import android.content.Context

/**
 * The interface for wrapping static [PermissionUtils] methods.
 */
interface PermissionUtilWrapper {
    fun hasPermissions(context: Context?, vararg permissions: String): Boolean =
        PermissionUtils.hasPermissions(context, *permissions)
}
