package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Implementation of GetAccountAchievements
 */
class DefaultGetAccountAchievements @Inject constructor(private val accountRepository: AccountRepository) :
    GetAccountAchievements {

    override suspend fun invoke(
        achievementType: AchievementType,
        awardIndex: Long,
    ): MegaAchievement? {
        return if (accountRepository.isAccountAchievementsEnabled()) {
            accountRepository.getAccountAchievements(achievementType = achievementType,
                awardIndex = awardIndex)
        } else {
            null
        }
    }
}