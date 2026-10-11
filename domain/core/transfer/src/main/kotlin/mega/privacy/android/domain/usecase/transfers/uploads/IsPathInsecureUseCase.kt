package mega.privacy.android.domain.usecase.transfers.uploads

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to check if a path is insecure.
 *
 * @property fileSystemRepository Repository to provide the related data.
 */
class IsPathInsecureUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke the use case.
     *
     * @param path The path to check.
     * @return True if the path is insecure, false otherwise.
     */
    suspend operator fun invoke(path: String): Boolean = fileSystemRepository.isPathInsecure(path)
}