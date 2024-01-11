package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Create New Image Uri Use Case
 *
 */
class CreateNewImageUriUseCase @Inject constructor(
    private val repository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(fileName: String) = repository.createNewImageUri(fileName)
}