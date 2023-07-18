package mega.privacy.android.domain.usecase.filelink

import mega.privacy.android.domain.repository.FileLinkRepository
import javax.inject.Inject

/**
 * Encrypt Link With Password Use Case
 *
 */
class EncryptLinkWithPasswordUseCase @Inject constructor(
    private val fileLinkRepository: FileLinkRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(link: String, password: String) =
        fileLinkRepository.encryptLinkWithPassword(link, password)
}