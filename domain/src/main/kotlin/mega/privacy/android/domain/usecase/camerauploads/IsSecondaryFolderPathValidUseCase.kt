package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.FileSystemRepository
import java.nio.file.Paths
import javax.inject.Inject

/**
 * Use Case that checks whether the Secondary Folder path is valid or not
 *
 * @property getPrimaryFolderPathUseCase [GetPrimaryFolderPathUseCase]
 * @property fileSystemRepository [FileSystemRepository]
 */
class IsSecondaryFolderPathValidUseCase @Inject constructor(
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invocation function
     *
     * @param path The Secondary Folder path, which can be nullable
     * @return true if the Secondary Folder exists and is different from the Primary Folder
     * local path. Otherwise, return false
     */
    suspend operator fun invoke(path: String?): Boolean =
        if (!path.isNullOrBlank() && fileSystemRepository.doesFolderExists(path)) {
            val primaryFolderPath = getPrimaryFolderPathUseCase()

            if (primaryFolderPath.isNotBlank()) {
                val primaryAbsolutePath = Paths.get(primaryFolderPath).toAbsolutePath()
                val secondaryAbsolutePath = Paths.get(path).toAbsolutePath()

                !secondaryAbsolutePath.startsWith(primaryAbsolutePath) &&
                        !primaryAbsolutePath.startsWith(secondaryAbsolutePath)
            } else {
                // An empty Primary Folder Path automatically makes the Secondary Folder Path valid
                true
            }
        } else false
}
