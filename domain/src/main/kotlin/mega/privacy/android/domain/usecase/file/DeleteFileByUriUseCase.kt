package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Delete File By Uri Use Case
 *
 */
class DeleteFileByUriUseCase @Inject constructor(
    private val repository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(uri: String) = repository.deleteFileByUri(uri)
}