package mega.privacy.android.app.utils.permission

import android.content.Context
import javax.inject.Inject

/**
 * The implementation of [PermissionUtilWrapper]
 */
class PermissionUtilFacade @Inject constructor() : PermissionUtilWrapper {

    override fun hasPermissions(context: Context?, vararg permissions: String) =
        PermissionUtils.hasPermissions(context, *permissions)
}
