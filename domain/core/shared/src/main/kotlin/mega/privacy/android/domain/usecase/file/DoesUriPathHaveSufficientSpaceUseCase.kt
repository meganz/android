package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import javax.inject.Inject

/**
 * Does uri path have sufficient space
 */
class DoesUriPathHaveSufficientSpaceUseCase @Inject constructor(
    private val getDiskSpaceBytesUseCase: GetDiskSpaceBytesUseCase,
) {
    /**
     * Invoke
     *
     * @param uriPath
     * @param requiredSpace
     * @return true if path has sufficient space or disk space is unknown, otherwise false
     */
    suspend operator fun invoke(uriPath: UriPath, requiredSpace: Long): Boolean = runCatching {
        val diskSpace = getDiskSpaceBytesUseCase(uriPath)
            ?: return@runCatching true // if disk space can't be known, assume there's enough space
        diskSpace > requiredSpace
    }.getOrDefault(false)
}