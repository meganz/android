package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that sets the default Primary Folder path
 */
class SetDefaultPrimaryFolderPathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase,
) {
    /**
     * Invocation function
     *
     * The Primary Folder path is set to the Local DCIM Folder path
     */
    suspend operator fun invoke() {
        if (fileSystemRepository.doesExternalStorageDirectoryExists()) {
            setPrimaryFolderLocalPathUseCase(fileSystemRepository.localDCIMFolderPath)
        }
    }
}
