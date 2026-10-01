package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to check if there is an installed app able to open a file with the given MIME type.
 */
class HasSuitableAppToOpenFileUseCase @Inject constructor(
    private val repository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param mimeType the MIME type of the file to be opened
     * @return true if a suitable app is available, false otherwise
     */
    suspend operator fun invoke(mimeType: String) =
        repository.hasSuitableAppToOpenFile(mimeType)
}
