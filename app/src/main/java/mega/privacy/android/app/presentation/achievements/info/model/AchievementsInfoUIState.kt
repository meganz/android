package mega.privacy.android.app.presentation.achievements.info.model

import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * UI State for AchievementsInfoFragment
 * @param awardId the award id of the received achievements
 * @param achievementType the type of Achievements
 * @param achievementRemainingDays remaining days on the achievements bonus
 * @param awardStorageInBytes awarded storage in bytes from the achievements
 * @param isAchievementAwarded is achievement has been awarded to the user
 * @param isAchievementExpired is the awarded achievement expired
 * @param isAchievementAlmostExpired is the awarded achievement almost expired (below 15 days)
 */
data class AchievementsInfoUIState(
    val awardId: Int = -1,
    val achievementType: AchievementType? = null,
    val achievementRemainingDays: Long = 0,
    val awardStorageInBytes: Long = 0,
    val isAchievementAwarded: Boolean = false,
    val isAchievementExpired: Boolean = false,
    val isAchievementAlmostExpired: Boolean = false
)