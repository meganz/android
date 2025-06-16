package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject
import kotlin.time.ExperimentalTime

/**
 * Use case to get the last modified time of a [UriPath].
 *
 */
class GetLastModifiedTimeUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke.
     */
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(uriPath: UriPath) =
        fileSystemRepository.getLastModifiedTime(uriPath)
}