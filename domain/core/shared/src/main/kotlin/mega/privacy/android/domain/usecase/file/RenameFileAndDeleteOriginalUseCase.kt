package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that renames an existing File, deletes the original file and returns the new File
 *
 * @param fileSystemRepository Repository containing all File handling operations
 */
class RenameFileAndDeleteOriginalUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invocation function
     *
     * @param originalUriPath The original File's Uri path
     * @param newFilename The File name to use for the new File
     *
     * @return The renamed File
     */
    suspend operator fun invoke(
        originalUriPath: UriPath,
        newFilename: String,
    ) = fileSystemRepository.renameFileAndDeleteOriginal(originalUriPath, newFilename)
}