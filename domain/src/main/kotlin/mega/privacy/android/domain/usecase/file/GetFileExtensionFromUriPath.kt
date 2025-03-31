package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get file extension from an [UriPath].
 */
class GetFileExtensionFromUriPath @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(uriPath: UriPath) =
        when {
            uriPath.isPath() -> uriPath.value
            else -> fileSystemRepository.getFileNameFromUri(uriPath.value) ?: ""
        }.substringAfterLast('.', "")
}