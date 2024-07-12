package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to sync offline local files with database entries
 */
class SyncOfflineFilesUseCase @Inject constructor(
    private val clearOfflineUseCase: ClearOfflineUseCase,
    private val getOfflineFilesUseCase: GetOfflineFilesUseCase,
    private val nodeRepository: NodeRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        val offlineNodes = nodeRepository.getAllOfflineNodes()
        if (fileSystemRepository.getOfflineFolder().exists() && offlineNodes.isNotEmpty()) {
            getOfflineFilesUseCase(offlineNodes)
                .asSequence()
                .partition { it.value.exists() }
                .let { (existingFiles, removedFiles) ->
                    // Delete offline info from database if files don't exist
                    removedFiles
                        .map { it.key }
                        .takeIf { it.isNotEmpty() }
                        ?.let { nodeRepository.removeOfflineNodeByIds(it) }

                    // Delete empty folders and database entries
                    existingFiles
                        .filter { it.value.isDirectory }
                        .sortedByDescending { it.key } // To delete child folders first
                        .mapNotNull { (id, file) ->
                            takeIf { file.listFiles().isNullOrEmpty() }?.let {
                                file.delete()
                                id
                            }
                        }.takeIf { it.isNotEmpty() }
                        ?.let { nodeRepository.removeOfflineNodeByIds(it) }
                }
        } else if (offlineNodes.isNotEmpty()) {
            clearOfflineUseCase()
        }
    }
}