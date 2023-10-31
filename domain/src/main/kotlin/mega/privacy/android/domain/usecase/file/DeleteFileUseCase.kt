package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case for deleting a local file.
 */
class DeleteFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    suspend operator fun invoke(filePath: String) {
        fileSystemRepository.deleteFile(File(filePath))
    }
}
