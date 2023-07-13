package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Create camera upload temporary root directory
 */
class CreateCameraUploadTemporaryRootDirectoryUseCase @Inject constructor(private val fileSystemRepository: FileSystemRepository) {

    /**
     * invoke
     * @return created directory path [String]
     */
    suspend operator fun invoke(): String =
        fileSystemRepository.createCameraUploadTemporaryRootDirectory()?.let {
            "${it.absolutePath}${File.separator}"
        } ?: DEFAULT_PATH

    companion object {
        /**
         * Default Path
         */
        private const val DEFAULT_PATH = ""
    }
}
