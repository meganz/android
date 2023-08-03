package mega.privacy.android.domain.usecase.filelink

import mega.privacy.android.domain.repository.FileLinkRepository
import javax.inject.Inject

/**
 * The use case for get file url from public link by http server
 */
class GetFileUrlByPublicLinkUseCase @Inject constructor(
    private val fileLinkRepository: FileLinkRepository,
) {
    /**
     * Invoke
     *
     * @param link public link
     * @return local link
     */
    suspend operator fun invoke(link: String) = fileLinkRepository.getFileUrlByPublicLink(link)
}