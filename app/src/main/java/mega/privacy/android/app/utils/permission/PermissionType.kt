package mega.privacy.android.app.utils.permission

import android.content.Context
import mega.privacy.android.app.utils.permission.PermissionUtils.hasSelfPermissions

/**
 * Definition of Permission Request Type
 */
sealed class PermissionType {

    /**
     * Normal process of permission check. It will create a fragment
     * Don't use it in onResume(), It will cause an endless loop
     */
    object NormalPermission : PermissionType() {

        override fun checkPermissions(context: Context, permissions: ArrayList<String>): Boolean =
            hasSelfPermissions(context, permissions)

        override fun fragment(permissions: ArrayList<String>): PermissionFragment =
            PermissionFragment.NormalRequestFragment.newInstance(
                permissions
            )
    }

    /**
     * Only check the permission and return result
     * True: Granted
     * False: Denied
     */
    object CheckPermission : PermissionType() {

        override fun checkPermissions(context: Context, permissions: ArrayList<String>): Boolean =
            hasSelfPermissions(context, permissions)

        override fun fragment(permissions: ArrayList<String>): PermissionFragment {
            return PermissionFragment.CheckRequestFragment.newInstance()
        }
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
