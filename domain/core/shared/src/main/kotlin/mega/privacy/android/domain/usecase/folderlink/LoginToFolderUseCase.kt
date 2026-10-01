package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.repository.FolderLinkRepository
import javax.inject.Inject

/**
 * Use case for logging into folder
 */
class LoginToFolderUseCase @Inject constructor(private val folderLinkRepository: FolderLinkRepository) {
    /**
     * Invoke
     *
     * @param folderLink    Link of the folder to login
     */
    suspend operator fun invoke(folderLink: String): FolderLoginStatus =
        folderLinkRepository.loginToFolder(folderLink)
}