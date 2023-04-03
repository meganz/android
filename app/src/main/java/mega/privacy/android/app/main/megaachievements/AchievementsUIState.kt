package mega.privacy.android.app.main.megaachievements

import mega.privacy.android.domain.entity.achievement.AchievementsOverview

/**
 * UI State for achievement composable
 *
 * @property achievementsOverview Achievements overview
 * @property areAllRewardsExpired Are all rewards expired
 * @property showError Show error state
 * @property showAddPhoneReward Show add phone reward section
 *
 **/
data class AchievementsUIState(
    val achievementsOverview: AchievementsOverview? = null,
    val areAllRewardsExpired: Boolean? = null,
    val showError: Boolean = false,
    val showAddPhoneReward: Boolean = false,
)
