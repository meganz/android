package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case to get the storage type name: Device Model or SD Card based on file location
 */
class GetFileStorageTypeNameUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param file
     */
    suspend operator fun invoke(file: File) = fileSystemRepository.getFileStorageTypeName(file)
}