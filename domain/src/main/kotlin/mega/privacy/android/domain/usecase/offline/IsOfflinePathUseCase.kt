package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Check if a Transfer is to downloading a node for offline use
 */
class IsOfflinePathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Check if the [path] is in the offline directory, either ordinary or backups offline
     * @param path The path to check. It should be a file path, never a Uri, as it never comes from user picker or third party app.
     * @return true if it's a path in the offline folders, false otherwise
     */
    suspend operator fun invoke(path: String) =
        path.startsWith(fileSystemRepository.getOfflinePath())
                || path.startsWith(fileSystemRepository.getOfflineBackupsPath())
}