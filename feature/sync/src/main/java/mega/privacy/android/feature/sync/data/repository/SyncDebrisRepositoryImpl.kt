package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.SyncDebrisGateway
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncDebris
import mega.privacy.android.feature.sync.domain.repository.SyncDebrisRepository
import timber.log.Timber
import javax.inject.Inject

internal class SyncDebrisRepositoryImpl @Inject constructor(
    private val fileGateway: FileGateway,
    private val syncDebrisGateway: SyncDebrisGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val documentFileWrapper: DocumentFileWrapper,
) : SyncDebrisRepository {

    override suspend fun clear() {
        withContext(ioDispatcher) {
            syncDebrisGateway.get().forEach { debris ->
                documentFileWrapper.getDocumentFileForSyncContentUri(debris.path.value).let {
                    Timber.d("Deleting debris  folder: $debris, uri: ${it?.uri}")
                    it?.delete() == true
                }
            }.also {
                syncDebrisGateway.set(emptyList())
            }
        }
    }

    override suspend fun getSyncDebrisForSyncs(syncs: List<FolderPair>): List<SyncDebris> =
        withContext(ioDispatcher) {
            syncs.mapNotNull { sync ->
                val syncDebrisFolder =
                    fileGateway.findFileInDirectory(
                        UriPath(sync.localFolderPath),
                        DEBRIS_FOLDER_NAME
                    ) ?: return@mapNotNull null
                val debrisSize =
                    fileGateway.getFilesInDocumentFolder(UriPath(syncDebrisFolder.uri.toString()))
                        .files.sumOf { it.size }
                SyncDebris(
                    syncId = sync.id,
                    path = UriPath(syncDebrisFolder.uri.toString()),
                    sizeInBytes = debrisSize
                )
            }.also(syncDebrisGateway::set)
        }

    private companion object {
        private const val DEBRIS_FOLDER_NAME = ".debris"
    }
}
