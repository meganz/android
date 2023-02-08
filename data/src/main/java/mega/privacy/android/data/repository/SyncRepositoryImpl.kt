package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.domain.entity.sync.RemoteFolder
import mega.privacy.android.domain.repository.SyncRepository
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor() : SyncRepository {

    private val localPath = MutableSharedFlow<String>(1)
    private val remotePath = MutableSharedFlow<RemoteFolder>(1)

    override fun setSyncLocalPath(path: String) {
        localPath.tryEmit(path)
    }

    override fun getSyncLocalPath(): Flow<String> = localPath

    override fun setSyncRemotePath(path: RemoteFolder) {
        remotePath.tryEmit(path)
    }

    override fun getSyncRemotePath(): Flow<RemoteFolder> = remotePath
}
