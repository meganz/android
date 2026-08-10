package mega.privacy.android.domain.usecase.filelink

import mega.privacy.android.domain.repository.LinksRepository
import javax.inject.Inject

/**
 * Decrypt Link With Password Use Case
 *
 */
class DecryptPasswordProtectedLinkUseCase @Inject constructor(
    private val linksRepository: LinksRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(passwordProtectedLink: String, password: String) =
        linksRepository.decryptPasswordProtectedLink(passwordProtectedLink, password)
}