package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get offline folder size use case
 */
class GetOfflineFolderSizeUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Long {
        val offlineFolder = fileSystemRepository.getOfflineFolder()
        if (offlineFolder.exists()) {
            return fileSystemRepository.getTotalSize(offlineFolder)
        }
        return 0
    }
}