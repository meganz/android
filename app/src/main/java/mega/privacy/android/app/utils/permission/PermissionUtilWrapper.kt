package mega.privacy.android.app.utils.permission

/**
 * The interface for wrapping static [PermissionUtils] methods.
 */
interface PermissionUtilWrapper {
    /**
     * Provide hasPermissions implementation
     */
    fun hasPermissions(vararg permissions: String): Boolean

    /**
     * Provide getImagePermissionByVersion implementation
     */
    fun getImagePermissionByVersion(): String

    /**
     * Provide getVideoPermissionByVersion implementation
     */
    fun getVideoPermissionByVersion(): String

    /**
     * Provide getAudioPermissionByVersion implementation
     */
    fun getAudioPermissionByVersion(): String
}
