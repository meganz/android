package mega.privacy.android.domain.repository

/**
 * Permission repository
 *
 */
interface PermissionRepository {
    /**
     * Has media permission
     */
    fun hasMediaPermission(): Boolean
}