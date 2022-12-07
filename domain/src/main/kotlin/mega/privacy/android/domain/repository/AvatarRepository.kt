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
    suspend fun getMyAvatarFile(): File?

    /**
     * Get avatar file for an user
     *
     * @param userEmail  User email
     */
    suspend fun getAvatarFile(userEmail: String): File?

    /**
     * Get avatar file for an user
     *
     * @param userHandle  User handle
     */
    suspend fun getAvatarFile(userHandle: Long): File?

    /**
     * Get avatar color for an user
     *
     * @param userHandle  User handle
     */
    suspend fun getAvatarColor(userHandle: Long): Int
}