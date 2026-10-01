package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get account achievements
 */
class GetAccountAchievements @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     *
     * @param achievementType = AchievementType
     * @param awardIndex = Long
     * @return MegaAchievement
     */
    suspend operator fun invoke(
        achievementType: AchievementType,
        awardIndex: Long,
    ): MegaAchievement? {
        return if (accountRepository.areAccountAchievementsEnabled()) {
            accountRepository.getAccountAchievements(
                achievementType = achievementType,
                awardIndex = awardIndex
            )
        } else {
            null
        }
    }
}
