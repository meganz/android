package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.sync.RemoteFolder

/**
 * repository for sync feature
 */
interface SyncRepository {

    /**
     * saves the the path to local folder that will be synced
     */
    fun setSyncLocalPath(localPath: String)

    /**
     * get the path to local folder that will be synced
     */
    fun getSyncLocalPath(): Flow<String>

    /**
     * saves the the path to remote folder that will be synced
     */
    fun setSyncRemotePath(remotePath: RemoteFolder)

    /**
     * get the path to remote folder that will be synced
     */
    fun getSyncRemotePath(): Flow<RemoteFolder>
}
