package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Is video file use case
 *
 * @property fileSystemRepository
 * @constructor Create empty Is vide file use case
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
    suspend operator fun invoke(localPath: String) =
        fileSystemRepository.getGuessContentTypeFromName(localPath)?.startsWith("video") == true
}