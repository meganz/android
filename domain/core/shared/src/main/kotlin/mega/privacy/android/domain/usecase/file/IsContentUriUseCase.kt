package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to check if an uri string represents a content uri.
 */
class IsContentUriUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @param uriString
     * @return true if the [uriString] represents an external storage content Uri.
     */
    suspend operator fun invoke(uriString: String) =
        fileSystemRepository.isContentUri(uriString)
}