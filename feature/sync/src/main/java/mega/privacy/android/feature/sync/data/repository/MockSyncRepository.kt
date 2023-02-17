package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.FolderPairState
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

// This is a temporary repository created solely for successful project built
// It will be replaced by a real implementation in the next task
internal class MockSyncRepository @Inject constructor() : SyncRepository {

    override suspend fun setupFolderPair(localPath: String, remoteFolderId: Long) {
        // mock method
    }

    override suspend fun getFolderPairs(): List<FolderPair> = listOf(FolderPair(
        id = 1L,
        pairName = "Camera",
        localFolderPath = "/storage/emulated/0/DCIM/Camera",
        remoteFolder = RemoteFolder(1L, "android_sync"),
        state = FolderPairState.RUNNING,
    ))


    override suspend fun removeFolderPairs() {
        // mock method
    }

    override fun observeSyncState(): Flow<FolderPairState> = flowOf(FolderPairState.RUNNING)
}