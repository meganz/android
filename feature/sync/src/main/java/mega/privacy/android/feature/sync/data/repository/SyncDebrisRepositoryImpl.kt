package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.SyncDebrisGateway
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncDebris
import mega.privacy.android.feature.sync.domain.repository.SyncDebrisRepository
import javax.inject.Inject

internal class SyncDebrisRepositoryImpl @Inject constructor(
    private val fileGateway: FileGateway,
    private val syncDebrisGateway: SyncDebrisGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncDebrisRepository {

    override suspend fun clear() {
        withContext(ioDispatcher) {
            syncDebrisGateway.get().forEach { debris ->
                fileGateway.deleteDirectory(debris.path)
            }.also {
                syncDebrisGateway.set(emptyList())
            }
        }
    }

    override suspend fun getSyncDebrisForSyncs(syncs: List<FolderPair>): List<SyncDebris> =
        withContext(ioDispatcher) {
            syncs.mapNotNull { sync ->
                val syncDebrisFolder =
                    fileGateway.findFileInDirectory(sync.localFolderPath, DEBRIS_FOLDER_NAME)
                syncDebrisFolder?.let {
                    SyncDebris(
                        syncId = sync.id,
                        path = syncDebrisFolder.absolutePath,
                        sizeInBytes = fileGateway.getTotalSize(syncDebrisFolder)
                    )
                }
            }.also(syncDebrisGateway::set)
        }

    private companion object {
        private const val DEBRIS_FOLDER_NAME = ".debris"
    }
}