package mega.privacy.android.app.main.megaachievements

import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * UI State for InfoAchievementsFragment
 * @see InfoAchievementsFragment
 * @property awardId the award id of the received achievements
 * @property achievementType the type of Achievements
 * @property achievementRemainingDays remaining days on the achievements bonus
 * @property awardStorageInBytes awarded storage in bytes from the achievements
 */
data class InfoAchievementsUIState(
    val awardId: Int = -1,
    val achievementType: AchievementType? = null,
    val achievementRemainingDays: Long = 0,
    val awardStorageInBytes: Long = 0
)