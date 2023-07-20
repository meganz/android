package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Is image file use case
 *
 * @property fileSystemRepository
 * @constructor Create empty Is image file use case
 */
class IsImageFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @param localPath
     * @return True if the file is an image, false otherwise.
     */
    suspend operator fun invoke(localPath: String) =
        fileSystemRepository.getGuessContentTypeFromName(localPath)?.startsWith("image") == true
}