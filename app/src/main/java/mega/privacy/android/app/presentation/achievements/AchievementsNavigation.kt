package mega.privacy.android.app.presentation.achievements

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.achievements.info.view.AchievementsInfoRoute
import mega.privacy.android.app.presentation.achievements.view.AchievementRoute
import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * Route for [AchievementsInfoRoute]
 */
internal const val achievementsRoute = "achievements/main"

/**
 * Composable destination for [AchievementRoute]
 */
fun NavGraphBuilder.achievementScreen(
    onNavigateToInviteFriends: (Long) -> Unit,
    onNavigateToInfoAchievements: (achievementType: AchievementType) -> Unit,
    onNavigateToReferralBonuses: () -> Unit,
) {
    composable(achievementsRoute) {
        AchievementRoute(
            onNavigateToInfoAchievements = onNavigateToInfoAchievements,
            onNavigateToInviteFriends = onNavigateToInviteFriends,
            onNavigateToReferralBonuses = onNavigateToReferralBonuses
        )
    }
}

/**
 * Navigation for [AchievementRoute]
 */
fun NavController.navigateToAchievements(navOptions: NavOptions? = null) {
    this.navigate(route = achievementsRoute, navOptions = navOptions)
}