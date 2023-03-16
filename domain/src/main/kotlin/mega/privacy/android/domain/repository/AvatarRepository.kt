package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Avatar Repository
 */
interface AvatarRepository {
    /**
     * get avatar file
     */
    fun monitorMyAvatarFile(): Flow<File?>

    /**
     * Get color avatar
     */
    suspend fun getMyAvatarColor(): Int

    /**
     * get avatar file
     */
    suspend fun getMyAvatarFile(isForceRefresh: Boolean = false): File?

    /**
     * Get avatar file for an user
     *
     * @param userEmail  User email
     * @param skipCache  Skip cached avatar.
     */
    suspend fun getAvatarFile(userEmail: String, skipCache: Boolean = false): File

    /**
     * Get avatar file for an user
     *
     * @param userHandle  User handle
     * @param skipCache  Skip cached avatar.
     */
    suspend fun getAvatarFile(userHandle: Long, skipCache: Boolean = false): File

    /**
     * Get avatar color for an user
     *
     * @param userHandle  User handle
     */
    suspend fun getAvatarColor(userHandle: Long): Int

    /**
     * Set avatar
     *
     * @param filePath
     */
    suspend fun setAvatar(filePath: String?)

    /**
     * Update my avatar with new email
     *
     * @param oldEmail
     * @param newEmail
     * @return true if success otherwise false
     */
    suspend fun updateMyAvatarWithNewEmail(oldEmail: String, newEmail: String): Boolean
}