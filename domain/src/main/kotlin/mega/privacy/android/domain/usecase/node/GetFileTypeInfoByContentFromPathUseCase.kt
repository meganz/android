package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Detects the real [FileTypeInfo] of a local file from its content (magic numbers), reading only
 * a header. Used to identify local files (e.g. offline files) that have no extension.
 *
 * @return the detected [FileTypeInfo], or null when the content cannot be recognised.
 */
class GetFileTypeInfoByContentFromPathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param localFilePath absolute path to the local file
     * @param duration duration of the file, when known
     */
    suspend operator fun invoke(localFilePath: String, duration: Int = 0): FileTypeInfo? {
        val header = fileSystemRepository
            .readFirstBytesFromPath(
                localFilePath,
                GetFileTypeInfoByContentUseCase.HEADER_SIZE_BYTES
            )
            ?.takeIf { it.isNotEmpty() } ?: return null
        return fileSystemRepository.getFileTypeInfoFromContent(header, duration)
    }
}
