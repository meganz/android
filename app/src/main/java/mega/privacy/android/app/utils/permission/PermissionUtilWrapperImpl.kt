package mega.privacy.android.app.utils.permission

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Permission util wrapper impl
 *
 * @property context
 */
class PermissionUtilWrapperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionUtilWrapper {

    override fun hasPermissions(vararg permissions: String): Boolean =
        PermissionUtils.hasPermissions(context, *permissions)

    /**
     * Provide getImagePermissionByVersion implementation
     */
    override fun getImagePermissionByVersion() = PermissionUtils.getImagePermissionByVersion()

    /**
     * Provide getVideoPermissionByVersion implementation
     */
    override fun getVideoPermissionByVersion() = PermissionUtils.getVideoPermissionByVersion()

    /**
     * Provide getAudioPermissionByVersion implementation
     */
    override fun getAudioPermissionByVersion() = PermissionUtils.getAudioPermissionByVersion()
}