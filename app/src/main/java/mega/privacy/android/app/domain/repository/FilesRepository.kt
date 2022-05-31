package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FolderVersionInfo
import mega.privacy.android.app.domain.exception.MegaException
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
    @Throws(MegaException::class)
    suspend fun getRootFolderVersionInfo(): FolderVersionInfo

    /**
     * Monitor node updates
     *
     * @return a flow of all global node updates
     */
    fun monitorNodeUpdates(): Flow<List<MegaNode>>
}
