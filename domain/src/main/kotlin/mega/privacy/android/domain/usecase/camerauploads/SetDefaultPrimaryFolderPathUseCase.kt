package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that sets the default Primary Folder path
 */
class SetDefaultPrimaryFolderPathUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase,
) {
    /**
     * Invocation function
     *
     * The Primary Folder path is set to the Local DCIM Folder path. In addition, the Primary Folder
     * is no longer in the SD Card, with its SD Card path invalidated
     */
    suspend operator fun invoke() {
        if (fileSystemRepository.doesExternalStorageDirectoryExists()) {
            with(cameraUploadRepository) {
                setPrimaryFolderInSDCard(false)
                setPrimaryFolderSDCardUriPath("")
            }
            setPrimaryFolderLocalPathUseCase(fileSystemRepository.localDCIMFolderPath)
        }
    }
}