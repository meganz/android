package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.media.DoesAlbumsHaveLinksUseCase
import mega.privacy.android.domain.usecase.node.publiclink.DoesHaveLinksUseCase
import javax.inject.Inject

/**
 * The use case to determine to show copyright or not
 */
class ShouldShowCopyrightUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val doesHaveLinksUseCase: DoesHaveLinksUseCase,
    private val doesAlbumsHaveLinksUseCase: DoesAlbumsHaveLinksUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() =
        accountRepository.shouldShowCopyright() && !doesHaveLinksUseCase() && !doesAlbumsHaveLinksUseCase()
}
