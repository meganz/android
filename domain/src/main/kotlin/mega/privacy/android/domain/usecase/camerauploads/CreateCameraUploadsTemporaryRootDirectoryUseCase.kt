package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use Case that creates a temporary root directory for Camera Uploads
 *
 * @property fileSystemRepository Repository containing all File related operations
 */
class CreateCameraUploadsTemporaryRootDirectoryUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invocation function
     *
     * @return The temporary root directory path
     */
    suspend operator fun invoke(): String =
        fileSystemRepository.createCameraUploadsTemporaryRootDirectory()?.let {
            "${it.absolutePath}${File.separator}"
        } ?: ""
}
