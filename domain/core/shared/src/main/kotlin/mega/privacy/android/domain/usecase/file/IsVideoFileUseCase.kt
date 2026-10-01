package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Is video file use case
 *
 * @property fileSystemRepository
 * @constructor Create empty Is video file use case
 */
class IsVideoFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @param localPath
     * @return True if the file is a video, false otherwise.
     */
    suspend operator fun invoke(uriPath: UriPath) =
        listOf(
            fileSystemRepository.getGuessContentTypeFromName(uriPath.value),
            fileSystemRepository.getContentTypeFromContentUri(uriPath)
        ).any { it?.startsWith("video") == true }
}