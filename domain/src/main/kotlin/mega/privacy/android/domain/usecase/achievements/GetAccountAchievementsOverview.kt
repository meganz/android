package mega.privacy.android.domain.usecase.achievements

import mega.privacy.android.domain.entity.achievement.AchievementsOverview

/**
 * Use case to get an overview of all the existing achievements
 * and rewards (unlocked achievements) for current user.
 */
fun interface GetAccountAchievementsOverview {

    /**
     * Invoke.
     * @return The overview of all the existing achievements and rewards.
     */
    suspend operator fun invoke(): AchievementsOverview
}
