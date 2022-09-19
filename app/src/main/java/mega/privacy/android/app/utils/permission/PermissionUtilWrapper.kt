package mega.privacy.android.app.utils.permission

import android.content.Context

/**
 * The interface for wrapping static [PermissionUtils] methods.
 */
interface PermissionUtilWrapper {
    /**
     * Provide hasPermissions implementation
     */
    fun hasPermissions(context: Context?, vararg permissions: String): Boolean =
        PermissionUtils.hasPermissions(context, *permissions)

    /**
     * Provide getImagePermissionByVersion implementation
     */
    fun getImagePermissionByVersion() = PermissionUtils.getImagePermissionByVersion()

    /**
     * Provide getVideoPermissionByVersion implementation
     */
    fun getVideoPermissionByVersion() = PermissionUtils.getVideoPermissionByVersion()

    /**
     * Provide getAudioPermissionByVersion implementation
     */
    fun getAudioPermissionByVersion() = PermissionUtils.getAudioPermissionByVersion()
}
