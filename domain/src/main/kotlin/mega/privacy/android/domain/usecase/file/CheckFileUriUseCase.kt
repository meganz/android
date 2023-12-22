package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case for checking if file exists by uri path.
 */
class CheckFileUriUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Check if file exists by uri path
     *
     * @param uriPath Uri path
     * @return File path if exists, null otherwise
     */
    suspend operator fun invoke(uriPath: String?): String? =
        fileSystemRepository.checkFileExistsByUriPath(uriPath)
}

