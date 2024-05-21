package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import java.io.File
import javax.inject.Inject

/**
 * Use Case that retrieves the Primary Folder path
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class GetPrimaryFolderPathUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {
    /**
     * Invocation function
     *
     * @return The current Primary Folder path
     */
    suspend operator fun invoke(): String {
        with(cameraUploadsRepository) {
            return getPrimaryFolderLocalPath()?.addSeparator() ?: ""
        }
    }

    /**
     * Appends the separator "/" to the Primary Folder path if it does not exist.
     * e.g. "/storage/emulated/0/DCIM" becomes "storage/emulated/0/DCIM/"
     */
    private fun String.addSeparator(): String {
        val fileSeparator = File.separator
        return if (this.trim().isNotBlank() && !this.endsWith(fileSeparator)) {
            "$this$fileSeparator"
        } else this
    }
}
