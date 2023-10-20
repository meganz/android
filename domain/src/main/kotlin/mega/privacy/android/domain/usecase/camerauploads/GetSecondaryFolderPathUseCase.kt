package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use Case that retrieves the Secondary Folder path
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property fileSystemRepository [FileSystemRepository]
 */
class GetSecondaryFolderPathUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invocation function
     *
     * @return The current Secondary Folder path
     */
    suspend operator fun invoke(): String {
        val path = getLocalPath()
        return path.addSeparator()
    }

    /**
     * Retrieves the Secondary Folder local path
     *
     * @return the Secondary Folder local path
     */
    private suspend fun getLocalPath(): String {
        val localPath = cameraUploadRepository.getSecondaryFolderLocalPath()

        return if (localPath.isBlank() ||
            (localPath.isNotBlank() && fileSystemRepository.doesFolderExists(localPath))
        ) {
            localPath
        } else {
            cameraUploadRepository.setSecondaryFolderLocalPath("")
            ""
        }
    }

    /**
     * Appends the separator "/" to the Secondary Folder path if it does not exist.
     * e.g. "/storage/emulated/0/DCIM" becomes "storage/emulated/0/DCIM/"
     */
    private fun String.addSeparator(): String {
        val fileSeparator = File.separator
        return if (this.trim().isNotBlank() && !this.endsWith(fileSeparator)) {
            "$this$fileSeparator"
        } else this
    }
}
