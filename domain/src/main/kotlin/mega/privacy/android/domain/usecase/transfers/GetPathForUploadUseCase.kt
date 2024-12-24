package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.PermissionRepository
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UseContentUrisForUploadsUseCase
import java.io.File
import javax.inject.Inject

/**
 * Get a file for this uri or path that can be accessed by SDK:
 * - If the string is already representing an existing file path it returns the file path
 * - If the Uri is already representing a file it returns the file path
 * - If the Uri is a content uri, it makes a copy in the chat cache folder and returns its path
 */
class GetPathForUploadUseCase @Inject constructor(
    private val getCacheFileForUploadUseCase: GetCacheFileForUploadUseCase,
    private val doesPathHaveSufficientSpaceUseCase: DoesPathHaveSufficientSpaceUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val permissionRepository: PermissionRepository,
    private val useContentUrisForUploadsUseCase: UseContentUrisForUploadsUseCase,
) {
    /**
     * Invoke
     *
     * @param originalUriPath a string representing the UriPath of the file or folder to be uploaded
     * @return the uriPath of the file or folder to be uploaded, as String for testing purposes
     */
    suspend operator fun invoke(originalUriPath: UriPath, isChatUpload: Boolean): String? {
        return when {
            fileSystemRepository.isContentUri(originalUriPath.value)
                    && useContentUrisForUploadsUseCase(isChatUpload) -> {
                originalUriPath.value
            }

            fileSystemRepository.isFilePath(originalUriPath.value) -> {
                originalUriPath.value
            }

            fileSystemRepository.isFileUri(originalUriPath.value) -> {
                fileSystemRepository.getFileFromFileUri(originalUriPath.value).absolutePath
            }

            fileSystemRepository.isContentUri(originalUriPath.value) -> {
                val file = takeIf { permissionRepository.hasManageExternalStoragePermission() }
                    ?.let { fileSystemRepository.getFileFromUri(originalUriPath) }
                    ?: fileSystemRepository.getFileNameFromUri(originalUriPath.value)?.let {
                        getCacheFileForUploadUseCase(
                            file = File(it),
                            isChatUpload = isChatUpload,
                        )?.also { destination ->
                            val size = fileSystemRepository.getFileSizeFromUri(it) ?: 0L
                            if (!doesPathHaveSufficientSpaceUseCase(destination.parent, size)) {
                                throw NotEnoughStorageException()
                            }
                            fileSystemRepository.copyContentUriToFile(
                                originalUriPath,
                                destination
                            )
                        }
                    }
                file?.absolutePath
            }

            else -> {
                null
            }
        }
    }
}