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
}