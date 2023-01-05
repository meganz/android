package mega.privacy.android.app.main.megaachievements

import mega.privacy.android.domain.entity.achievement.AchievementsOverview

sealed interface AchievementsUI {

    object Progress : AchievementsUI

    data class Content(
        val achievementsOverview: AchievementsOverview,
        val areAllRewardsExpired: Boolean
    ) : AchievementsUI

    object Error : AchievementsUI
}
