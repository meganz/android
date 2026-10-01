package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Check whether Achievements are enabled or not
 */
class AreAccountAchievementsEnabledUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {

    /**
     * Invoke.
     * @return areAccountAchievementsEnabled as [Boolean]
     */
    suspend operator fun invoke(): Boolean = accountRepository.areAccountAchievementsEnabled()
}
