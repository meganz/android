package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Clear offline use case
 */
class ClearOfflineUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        val offlineFolder = fileSystemRepository.getOfflineFolder()
        fileSystemRepository.deleteFolderAndItsFiles(offlineFolder.path)
        nodeRepository.clearOffline()
    }
}