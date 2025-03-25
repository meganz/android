package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to check if a URI can be read.
 */
class CanReadUriUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke.
     *
     * @param stringUri URI to check.
     */
    suspend operator fun invoke(stringUri: String) =
        fileSystemRepository.canReadUri(stringUri)
}