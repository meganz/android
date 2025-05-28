package mega.privacy.android.app.presentation.achievements

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.achievements.info.view.AchievementsInfoRoute
import mega.privacy.android.app.presentation.achievements.view.AchievementRoute
import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * Route for [AchievementsInfoRoute]
 */
@Serializable
data object AchievementMain

/**
 * Composable destination for [AchievementRoute]
 */
fun NavGraphBuilder.achievementScreen(
    onNavigateToInviteFriends: (Long) -> Unit,
    onNavigateToInfoAchievements: (achievementType: AchievementType) -> Unit,
    onNavigateToReferralBonuses: () -> Unit,
    onNavigateToMegaVPNFreeTrial: (Boolean) -> Unit,
    onNavigateToMegaPassFreeTrial: (Boolean) -> Unit,
) {
    composable<AchievementMain> {
        AchievementRoute(
            onNavigateToInfoAchievements = onNavigateToInfoAchievements,
            onNavigateToInviteFriends = onNavigateToInviteFriends,
            onNavigateToReferralBonuses = onNavigateToReferralBonuses,
            onNavigateToMegaVPNFreeTrial = onNavigateToMegaVPNFreeTrial,
            onNavigateToMegaPassFreeTrial = onNavigateToMegaPassFreeTrial
        )
    }
}

/**
 * Navigation for [AchievementRoute]
 */
fun NavController.navigateToAchievements(navOptions: NavOptions? = null) {
    this.navigate(route = AchievementMain, navOptions = navOptions)
}