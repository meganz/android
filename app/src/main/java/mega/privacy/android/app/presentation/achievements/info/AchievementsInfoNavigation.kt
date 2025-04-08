package mega.privacy.android.app.presentation.achievements.info

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.achievements.info.view.AchievementsInfoRoute
import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * Route for [AchievementsInfoRoute]
 * @param achievementType
 */
@Serializable
data class AchievementMain(val achievementType: AchievementType)

/**
 * Composable destination for [AchievementsInfoRoute]
 */
fun NavGraphBuilder.achievementsInfoScreen() {
    composable<AchievementMain> {
        AchievementsInfoRoute()
    }
}

/**
 * Navigation for [AchievementsInfoRoute]
 */
fun NavController.navigateToAchievementsInfo(
    type: AchievementType,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = AchievementMain(type),
        navOptions = navOptions
    )
}