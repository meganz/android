package mega.privacy.android.domain.usecase.camerauploads

import java.nio.file.Paths
import javax.inject.Inject

/**
 * Use Case that checks whether or not the specific Local Secondary Folder from the Device File
 * Explorer is not the same Folder or a parent Folder or a sub Folder from the current Local
 * Primary Folder
 *
 * The Use Case assumes that this specific Local Secondary Folder exists through
 * [IsFolderPathExistingUseCase]
 *
 * @property getPrimaryFolderPathUseCase Retrieves the folder path of the current Local Primary Folder
 */
class IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase @Inject constructor(
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
) {

    /**
     * Invocation function
     *
     * @param newPath the validated Local Secondary Folder path
     *
     * @return true if the Local Secondary Folder is unrelated to the current Local Primary Folder
     */
    suspend operator fun invoke(newPath: String): Boolean {
        val primaryFolderPath = getPrimaryFolderPathUseCase()

        return if (primaryFolderPath.isNotBlank()) {
            val primaryAbsolutePath = Paths.get(primaryFolderPath).toAbsolutePath()
            val secondaryAbsolutePath = Paths.get(newPath).toAbsolutePath()

            !secondaryAbsolutePath.startsWith(primaryAbsolutePath) &&
                    !primaryAbsolutePath.startsWith(secondaryAbsolutePath)
        } else {
            // An empty Primary Folder Path automatically makes the Secondary Folder Path unrelated
            true
        }
    }
}