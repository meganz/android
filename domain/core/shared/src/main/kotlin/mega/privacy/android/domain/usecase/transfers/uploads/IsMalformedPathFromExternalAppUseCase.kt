package mega.privacy.android.domain.usecase.transfers.uploads

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to check if a path is malformed from an external app.
 */
class IsMalformedPathFromExternalAppUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke the use case.
     * Check if the path is malformed from an external app.
     *
     * @param action The Intent action.
     * @param path The path to check.
     */
    suspend operator fun invoke(action: String?, path: String): Boolean =
        fileSystemRepository.isMalformedPathFromExternalApp(action, path)
}