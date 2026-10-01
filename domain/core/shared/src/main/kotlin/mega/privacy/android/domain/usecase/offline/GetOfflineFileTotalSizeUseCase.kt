package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Get total size of an offline file/folder
 */
class GetOfflineFileTotalSizeUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param file [File]
     * @return [Long]
     */
    suspend operator fun invoke(file: File): Long {
        return fileSystemRepository.getTotalSize(file)
    }
}