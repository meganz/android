package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Create New Video Uri Use Case
 *
 */
class CreateNewVideoUriUseCase @Inject constructor(
    private val repository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(fileName: String) = repository.createNewVideoUri(fileName)
}