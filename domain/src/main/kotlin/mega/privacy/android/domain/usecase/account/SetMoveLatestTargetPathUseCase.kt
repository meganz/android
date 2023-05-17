package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Set latest target path of move
 */
class SetMoveLatestTargetPathUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(path: Long) =
        accountRepository.setLatestTargetPathMovePreference(path)
}