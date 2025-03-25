package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get the file name from a content Uri
 */
class GetFileNameFromStringUriUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    /**
     * Invoke
     *
     * @param uriOrPathString a string representing the Uri
     */
    suspend operator fun invoke(uriOrPathString: String): String? =
        fileSystemRepository.getFileNameFromUri(uriOrPathString)
}