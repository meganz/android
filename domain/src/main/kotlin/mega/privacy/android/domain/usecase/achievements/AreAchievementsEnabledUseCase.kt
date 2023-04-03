package mega.privacy.android.domain.usecase.achievements

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to check if achievements are enabled
 */

class AreAchievementsEnabledUseCase @Inject constructor(private val accountRepository: AccountRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() = accountRepository.areAccountAchievementsEnabled()
}
