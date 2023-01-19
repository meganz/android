package mega.privacy.android.app.main.megaachievements

import mega.privacy.android.domain.entity.achievement.AchievementsOverview

data class AchievementsUIState(
    val achievementsOverview: AchievementsOverview? = null,
    val areAllRewardsExpired: Boolean? = null,
    val showError: Boolean = false,
)
