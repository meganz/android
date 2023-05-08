package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get file versions option
 *
 */
class GetFileVersionsOption @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(forceRefresh: Boolean) =
        fileSystemRepository.getFileVersionsOption(forceRefresh)
}