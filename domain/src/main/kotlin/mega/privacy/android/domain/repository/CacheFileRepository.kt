package mega.privacy.android.domain.repository

import java.io.File

/**
 * Cache File Repository
 */
interface CacheFileRepository {

    /**
     * clean files from a specific directory
     */
    fun purgeDirectory(directory: File)


    /**
     * clean files from Cache directory
     */
    suspend fun purgeCacheDirectory()
}