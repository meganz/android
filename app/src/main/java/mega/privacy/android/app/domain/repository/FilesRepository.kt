package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FolderVersionInfo
import nz.mega.sdk.MegaNode

/**
 * Files repository
 *
 */
interface FilesRepository {
    /**
     * Get folder version info
     *
     * @return info
     */
    suspend fun getRootFolderVersionInfo(): FolderVersionInfo

    /**
     * Monitor node updates
     *
     * @return a flow of all global node updates
     */
    fun monitorNodeUpdates(): Flow<List<MegaNode>>

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend fun getCloudSortOrder(): Int
}
