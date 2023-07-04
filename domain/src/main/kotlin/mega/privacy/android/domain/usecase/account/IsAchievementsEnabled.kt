package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Is achievements enabled
 *
 */
class IsAchievementsEnabled @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = accountRepository.isAchievementsEnabled()
}