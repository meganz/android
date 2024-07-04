package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * The use case for getting the FileInfoType of a given name
 */
class GetFileTypeInfoByNameUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Get file type info for a given file name
     *
     * @param name file name
     * @param duration duration of the file
     * @return [FileTypeInfo] object
     */
    operator fun invoke(name: String, duration: Int = 0) =
        fileSystemRepository.getFileTypeInfoByName(name, duration)
}