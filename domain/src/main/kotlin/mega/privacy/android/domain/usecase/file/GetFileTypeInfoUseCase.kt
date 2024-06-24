package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use Case to get FileTypeInfo of a file
 */
class GetFileTypeInfoUseCase @Inject constructor(
    private val repository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param file [File] the file to get the type info
     */
    suspend operator fun invoke(file: File) = repository.getFileTypeInfo(file)
}