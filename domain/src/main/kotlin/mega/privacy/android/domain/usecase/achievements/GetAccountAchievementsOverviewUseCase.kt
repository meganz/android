package mega.privacy.android.domain.usecase.achievements

import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case to get an overview of all the existing achievements
 * and rewards (unlocked achievements) for current user.
 */
class GetAccountAchievementsOverviewUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke.
     * @return The overview of all the existing achievements and rewards.
     */
    suspend operator fun invoke(): AchievementsOverview =
        accountRepository.getAccountAchievementsOverview()
}
