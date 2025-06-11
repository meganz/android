package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Checks if an uri path exists
 *
 */
class DoesUriPathExistsUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Checks if an uri path exists
     *
     * @param uriPath the [UriPath] to check
     * @return true if the UriPath exists, false otherwise
     */
    suspend operator fun invoke(uriPath: UriPath) =
        fileSystemRepository.doesUriPathExist(uriPath)
}