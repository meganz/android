package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

class GetFileNameFromContentUri @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    /**
     * Invoke
     *
     * @param uriOrPathString a string representing the Uri
     */
    suspend operator fun invoke(uriOrPathString: String): String? =
        fileSystemRepository.takeIf { it.isContentUri(uriOrPathString) }
            ?.getFileNameFromUri(uriOrPathString)
}