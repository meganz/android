package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case for making a name suitable for a file name in the local filesystem.
 *
 * @property fileSystemRepository
 * @constructor Create empty Escape fs incompatible use case
 */
class EscapeFsIncompatibleUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke.
     *
     * @param fileName Name to convert (UTF8)
     * @param dstPath  Destination path
     * @return Converted name (UTF8)
     */
    suspend operator fun invoke(fileName: String, dstPath: String) =
        fileSystemRepository.escapeFsIncompatible(fileName, dstPath)
}