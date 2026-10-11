package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Get File Uri use case
 */
class GetFileUriUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(file: File, authority: String) =
        fileSystemRepository.getUriForFile(file, authority)
}