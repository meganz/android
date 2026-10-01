package mega.privacy.android.domain.usecase.camerauploads

import javax.inject.Inject

/**
 * Use Case that prepares the Primary Folder path. This is generally called before enabling
 * Camera Uploads or when displaying the Primary Folder path in the UI for the first time
 *
 * If the Primary Folder path is found to be invalid, the default Primary Folder path is set
 * through [SetDefaultPrimaryFolderPathUseCase]
 *
 * @property getPrimaryFolderPathUseCase [GetPrimaryFolderPathUseCase]
 * @property isPrimaryFolderPathValidUseCase [IsPrimaryFolderPathValidUseCase]
 * @property setDefaultPrimaryFolderPathUseCase [SetDefaultPrimaryFolderPathUseCase]
 */
class PreparePrimaryFolderPathUseCase @Inject constructor(
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase,
    private val setDefaultPrimaryFolderPathUseCase: SetDefaultPrimaryFolderPathUseCase,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() {
        val primaryFolderPath = getPrimaryFolderPathUseCase()
        if (!isPrimaryFolderPathValidUseCase(primaryFolderPath)) {
            setDefaultPrimaryFolderPathUseCase()
        }
    }
}