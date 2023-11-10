package mega.privacy.android.domain.usecase.camerauploads

import javax.inject.Inject

/**
 * Use Case that sets the new Primary Folder path
 * @property setPrimaryFolderLocalPathUseCase [SetPrimaryFolderPathUseCase]
 */
class SetPrimaryFolderPathUseCase @Inject constructor(
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase,
) {

    /**
     * Invocation function
     *
     * @param newFolderPath The new Primary Folder path
     */
    suspend operator fun invoke(newFolderPath: String) {
        setPrimaryFolderLocalPathUseCase(newFolderPath)
    }
}
