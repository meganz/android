package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import java.nio.file.Paths
import javax.inject.Inject

/**
 * Use Case that checks whether the Primary Folder path is valid or not
 *
 * @property getSecondaryFolderPathUseCase [GetSecondaryFolderPathUseCase]
 * @property fileSystemRepository [FileSystemRepository]
 * @property isSecondaryFolderEnabled [IsSecondaryFolderEnabled]
 */
class IsPrimaryFolderPathValidUseCase @Inject constructor(
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
) {

    /**
     * Invocation function
     *
     * @param path The Primary Folder path, which can be nullable
     * @return true if the Primary Folder exists and is different from the Secondary Folder
     * local path. Otherwise, return false
     */
    suspend operator fun invoke(path: String?): Boolean =
        if (!path.isNullOrBlank() && fileSystemRepository.doesFolderExists(path)) {
            if (isSecondaryFolderEnabled()) {
                val secondaryFolderPath = getSecondaryFolderPathUseCase()
                if (secondaryFolderPath.isNotBlank()) {
                    val primaryAbsolutePath = Paths.get(path).toAbsolutePath()
                    val secondaryAbsolutePath =
                        Paths.get(secondaryFolderPath).toAbsolutePath()

                    !primaryAbsolutePath.startsWith(secondaryAbsolutePath) &&
                            !secondaryAbsolutePath.startsWith(primaryAbsolutePath)
                } else {
                    // An empty Secondary Folder Path automatically makes the Primary Folder Path valid
                    true
                }
            } else {
                // When Secondary Folder uploads are disabled, it automatically makes the Primary
                // Folder path valid
                true
            }
        } else false
}

