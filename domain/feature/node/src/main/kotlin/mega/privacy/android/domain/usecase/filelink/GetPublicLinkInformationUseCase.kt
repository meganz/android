package mega.privacy.android.domain.usecase.filelink

import mega.privacy.android.domain.entity.FolderInfo
import mega.privacy.android.domain.repository.FolderLinkRepository
import javax.inject.Inject

/**
 * Use case to get folder info of public link
 *
 */
class GetPublicLinkInformationUseCase @Inject constructor(
    private val folderLinkRepository: FolderLinkRepository,
) {
    /**
     * invoke method
     * @param link url of the link
     * @return [FolderInfo] if the [link] is valid. Otherwise throws exception.
     */
    suspend operator fun invoke(link: String): FolderInfo =
        folderLinkRepository.getPublicLinkInformation(link)
}