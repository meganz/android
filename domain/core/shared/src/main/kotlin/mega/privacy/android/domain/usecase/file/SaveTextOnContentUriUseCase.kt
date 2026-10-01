package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Save text on content uri use case
 */
class SaveTextOnContentUriUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(uri: String, text: String) =
        fileSystemRepository.saveTextOnContentUri(uri, text)
}