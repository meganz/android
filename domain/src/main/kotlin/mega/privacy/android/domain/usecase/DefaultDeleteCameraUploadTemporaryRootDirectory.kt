package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * default implementation of [DeleteCameraUploadTemporaryRootDirectory]
 */
class DefaultDeleteCameraUploadTemporaryRootDirectory @Inject constructor(private val fileSystemRepository: FileSystemRepository) :
    DeleteCameraUploadTemporaryRootDirectory {
    override suspend fun invoke() = run {
        val root = "${
            File(
                fileSystemRepository.cacheDir,
                CU_CACHE_FOLDER
            ).absolutePath
        }${File.separator}"
        fileSystemRepository.deleteDirectory(root)
    }

    private companion object {
        /**
         * Camera Uploads Cache Folder
         */
        private const val CU_CACHE_FOLDER = "cu"
    }
}
