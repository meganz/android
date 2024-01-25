package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * The use case to determine to show copyright or not
 */
class ShouldShowCopyrightUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = accountRepository.shouldShowCopyright()
}
