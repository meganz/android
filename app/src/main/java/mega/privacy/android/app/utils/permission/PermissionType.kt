package mega.privacy.android.app.utils.permission

import android.content.Context
import mega.privacy.android.app.utils.permission.PermissionUtils.hasSelfPermissions

/**
 * Definition of Permission Request Type
 */
sealed class PermissionType {

    object NormalPermission : PermissionType() {

        override fun checkPermissions(context: Context, permissions: ArrayList<String>): Boolean =
            hasSelfPermissions(context, permissions)

        override fun fragment(permissions: ArrayList<String>): PermissionFragment =
            PermissionFragment.NormalRequestFragment.newInstance(
                permissions
            )
    }

    /**
     * Check all the permissions
     */
    abstract fun checkPermissions(context: Context, permissions: ArrayList<String>): Boolean

    /**
     * The Fragment that process permission request
     */
    abstract fun fragment(permissions: ArrayList<String>): PermissionFragment
}
