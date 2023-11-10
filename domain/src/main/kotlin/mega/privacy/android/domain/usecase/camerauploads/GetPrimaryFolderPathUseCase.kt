package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import java.io.File
import javax.inject.Inject

/**
 * Use Case that retrieves the Primary Folder path
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class GetPrimaryFolderPathUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invocation function
     *
     * @return The current Primary Folder path
     */
    suspend operator fun invoke(): String {
        with(cameraUploadRepository) {
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
