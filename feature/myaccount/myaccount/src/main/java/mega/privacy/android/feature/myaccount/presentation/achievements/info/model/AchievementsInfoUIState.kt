package mega.privacy.android.feature.myaccount.presentation.achievements.info.model

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
 * @param durationInDays the duration of the achievements in days
 */
data class AchievementsInfoUIState(
    val awardId: Int,
    val achievementType: AchievementType?,
    val achievementRemainingDays: Long,
    val awardStorageInBytes: Long,
    val isAchievementAwarded: Boolean,
    val isAchievementExpired: Boolean,
    val isAchievementAlmostExpired: Boolean,
    val durationInDays: Int
)
