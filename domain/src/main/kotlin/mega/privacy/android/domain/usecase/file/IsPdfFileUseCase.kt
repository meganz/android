package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

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
     * @param localPath
     * @return True if the file is a pdf, false otherwise.
     */
    suspend operator fun invoke(localPath: String) =
        fileSystemRepository.getGuessContentTypeFromName(localPath)
            ?.startsWith("application/pdf") == true
}