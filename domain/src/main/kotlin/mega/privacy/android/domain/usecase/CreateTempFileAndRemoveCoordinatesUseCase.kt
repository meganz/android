package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject


/**
 * Create temp file, remove coordinates and set the last modified time
 */
class CreateTempFileAndRemoveCoordinatesUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     * @param rootPath
     * @param filePath
     * @param destinationPath
     * @param timestamp
     * @return new created file path
     */
    suspend operator fun invoke(
        rootPath: String,
        filePath: String,
        destinationPath: String,
        timestamp: Long,
    ): String = fileSystemRepository.createTempFile(
        rootPath,
        filePath,
        destinationPath,
    ).apply {
        fileSystemRepository.removeGPSCoordinates(this)
        fileSystemRepository.setLastModified(this, timestamp)
    }
}
