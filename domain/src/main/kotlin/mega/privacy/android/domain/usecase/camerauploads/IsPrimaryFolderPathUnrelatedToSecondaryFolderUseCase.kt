package mega.privacy.android.domain.usecase.camerauploads

import java.nio.file.Paths
import javax.inject.Inject

/**
 * Use Case that checks whether or not the specific Local Primary Folder from the Device File
 * Explorer is not the same Folder or a parent Folder or a sub Folder from the current Local
 * Secondary Folder
 *
 * The Use Case assumes that this specific Local Primary Folder exists through
 * [IsFolderPathExistingUseCase]
 *
 * @property getSecondaryFolderPathUseCase Retrieves the folder path of the current Local Secondary
 * Folder
 * @property isMediaUploadsEnabledUseCase true if Secondary Media uploads are enabled for Camera Uploads
 */
class IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase @Inject constructor(
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
) {
    /**
     * Invocation function
     *
     * @param newPath the validated Local Primary Folder path
     *
     * @return true if the Local Primary Folder is unrelated to the current Local Secondary Folder
     */
    suspend operator fun invoke(newPath: String) = if (isMediaUploadsEnabledUseCase()) {
        val secondaryFolderPath = getSecondaryFolderPathUseCase()
        if (secondaryFolderPath.isNotBlank()) {
            val primaryAbsolutePath = Paths.get(newPath).toAbsolutePath()
            val secondaryAbsolutePath =
                Paths.get(secondaryFolderPath).toAbsolutePath()

            !primaryAbsolutePath.startsWith(secondaryAbsolutePath) &&
                    !secondaryAbsolutePath.startsWith(primaryAbsolutePath)
        } else {
            // An empty Secondary Folder Path automatically makes the Primary Folder Path unrelated
            true
        }
    } else {
        // When Secondary Folder uploads are disabled, it automatically makes the Primary Folder
        // Path unrelated
        true
    }
}