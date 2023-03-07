package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [CreateCameraUploadTemporaryRootDirectory]
 */
class DefaultCreateCameraUploadTemporaryRootDirectory @Inject constructor(private val fileSystemRepository: FileSystemRepository) :
    CreateCameraUploadTemporaryRootDirectory {
    override suspend fun invoke() = run {
        val root = "${
            File(
                fileSystemRepository.cacheDir,
                CU_CACHE_FOLDER
            ).absolutePath
        }${File.separator}"
        fileSystemRepository.createDirectory(root)
        root
    }

    private companion object {
        /**
         * Camera Uploads Cache Folder
         */
        private const val CU_CACHE_FOLDER = "cu"
    }
}
