package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.MegaAchievement

/**
 * Get account achievements
 */
fun interface GetAccountAchievements {

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
    ): MegaAchievement?
}