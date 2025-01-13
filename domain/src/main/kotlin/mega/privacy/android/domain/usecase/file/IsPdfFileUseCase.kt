package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject
import kotlin.text.startsWith

/**
 * Is pdf file use case
 *
 * @property fileSystemRepository
 * @constructor Create empty Is Pdf file use case
 */
class IsPdfFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @param uriPath
     * @return True if the file is a pdf, false otherwise.
     */
    suspend operator fun invoke(uriPath: UriPath) =
        listOf(
            fileSystemRepository.getGuessContentTypeFromName(uriPath.value),
            fileSystemRepository.getContentTypeFromContentUri(uriPath)
        ).any { it?.startsWith("application/pdf") == true }
}