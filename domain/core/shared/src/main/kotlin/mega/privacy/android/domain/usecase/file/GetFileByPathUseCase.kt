package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject


/**
 * Get file by path if it exists
 */
class GetFileByPathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @param path file path
     * @return file if it exists, otherwise null
     */
    suspend operator fun invoke(path: String) = fileSystemRepository.getFileByPath(path)
}
