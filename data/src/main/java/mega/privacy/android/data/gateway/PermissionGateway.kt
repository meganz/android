package mega.privacy.android.data.gateway

/**
 * Permission Gateway
 */
interface PermissionGateway {

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

    /**
     * Provide getPartialMediaPermission
     */
    fun getPartialMediaPermission(): String
}
