package mega.privacy.android.domain.usecase.camerauploads

import javax.inject.Inject

/**
 * Use Case that checks whether the Secondary Folder path is valid or not
 *
 * @property isFolderPathExistingUseCase Checks if the specific Local Folder exists or not
 * @property isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase Checks if the specific Local
 * Secondary Folder Path is not the same Folder or a parent Folder or a sub Folder from the current
 * Local Primary Folder
 */
class IsSecondaryFolderPathValidUseCase @Inject constructor(
    private val isFolderPathExistingUseCase: IsFolderPathExistingUseCase,
    private val isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase: IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase,
) {

    /**
     * Invocation function
     *
     * @param path The Local Secondary Folder path, which can be nullable
     * @return true if the Local Secondary Folder exists and is unrelated to the Local Primary
     * Folder
     */
    suspend operator fun invoke(path: String?) = path?.let {
        isFolderPathExistingUseCase(it) && isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(it)
    } ?: false
}
