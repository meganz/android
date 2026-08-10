package mega.privacy.android.domain.usecase.node.publiclink

import mega.privacy.android.domain.repository.filemanagement.ShareRepository
import javax.inject.Inject

/**
 * Checks if the account does have public links
 *
 */
class DoesHaveLinksUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = shareRepository.doesHaveLinks()
}
