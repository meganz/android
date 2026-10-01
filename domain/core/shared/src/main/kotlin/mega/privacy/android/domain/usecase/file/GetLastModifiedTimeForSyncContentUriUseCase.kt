package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject
import kotlin.time.ExperimentalTime

/**
 * Get last modified time by content URI use case
 *
 */
class GetLastModifiedTimeForSyncContentUriUseCase @Inject constructor(
    private val repository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param uriPath the URI path of the file
     */
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(uriPath: UriPath) =
        repository.getLastModifiedTimeForSyncContentUri(uriPath)
}
