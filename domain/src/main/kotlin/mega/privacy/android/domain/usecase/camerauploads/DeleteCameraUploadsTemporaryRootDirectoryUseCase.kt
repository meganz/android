package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that deletes the temporary root directory created by Camera Uploads
 *
 * @property fileSystemRepository [FileSystemRepository]
 */
class DeleteCameraUploadsTemporaryRootDirectoryUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invocation function
     *
     * @return true if the delete operation is successful, and false if otherwise
     */
    suspend operator fun invoke() =
        fileSystemRepository.deleteCameraUploadsTemporaryRootDirectory()
}