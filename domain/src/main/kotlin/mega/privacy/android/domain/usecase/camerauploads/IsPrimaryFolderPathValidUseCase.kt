package mega.privacy.android.domain.usecase.camerauploads

import javax.inject.Inject

/**
 * Use Case that checks whether the Primary Folder path is valid or not
 *
 * @property isFolderPathExistingUseCase Checks if the specific Local Folder exists or not
 * @property isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase Checks if the specific Local
 * Primary Folder Path is not the same Folder or a parent Folder or a sub Folder from the current
 * Local Secondary Folder
 */
class IsPrimaryFolderPathValidUseCase @Inject constructor(
    private val isFolderPathExistingUseCase: IsFolderPathExistingUseCase,
    private val isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase: IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase,
) {

    /**
     * Invocation function
     *
     * @param path The Local Primary Folder path, which can be nullable
     * @return true if the Local Primary Folder exists and is unrelated to the Local Secondary
     * Folder
     */
    suspend operator fun invoke(path: String?): Boolean = path?.let {
        isFolderPathExistingUseCase(it) && isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(it)
    } ?: false
}

