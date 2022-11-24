package mega.privacy.android.app.presentation.achievements

import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * UI state for AchievementInfo screen
 *
 * @param toolbarTitle : Toolbar title
 * @param achievementType : Achievement type
 * @param uiMegaAchievement : [UIMegaAchievement]
 * @param awardCount : Award count
 */
data class AchievementInfoUIState(
    val toolbarTitle: String = "",
    val achievementType: AchievementType = AchievementType.INVALID_ACHIEVEMENT,
    val uiMegaAchievement: UIMegaAchievement? = null,
    val awardCount: Long = 0L,
)
