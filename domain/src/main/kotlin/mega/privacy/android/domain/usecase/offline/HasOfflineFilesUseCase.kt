package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Has offline files use case
 *
 * @property fileSystemRepository
 */
class HasOfflineFilesUseCase @Inject constructor(private val fileSystemRepository: FileSystemRepository) {
    /**
     * Invoke
     *
     * @return true if offline files are present, else false
     */
    suspend operator fun invoke(): Boolean {
        val offlineFolder = File(fileSystemRepository.getOfflinePath())
        return offlineFolder.exists() && offlineFolder.listFiles().isNotEmpty()
    }
}