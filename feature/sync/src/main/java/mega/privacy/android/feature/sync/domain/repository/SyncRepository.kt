package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * repository for sync feature
 */
internal interface SyncRepository {

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
