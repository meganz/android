package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use Case that retrieves the Secondary Folder path
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property fileSystemRepository [FileSystemRepository]
 */
class GetSecondaryFolderPathUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
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
        val localPath = cameraUploadsRepository.getSecondaryFolderLocalPath()

        return if (localPath.isNullOrBlank() ||
            (localPath.isNotBlank() && fileSystemRepository.doesFolderExists(localPath))
        ) {
            localPath ?: ""
        } else {
            cameraUploadsRepository.setSecondaryFolderLocalPath("")
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
