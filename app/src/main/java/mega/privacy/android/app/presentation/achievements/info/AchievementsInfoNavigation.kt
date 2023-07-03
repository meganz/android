package mega.privacy.android.app.presentation.achievements.info

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.achievements.info.view.AchievementsInfoRoute
import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * Route for [AchievementsInfoRoute]
 */
internal const val achievementsInfoRoute = "achievements/info"
internal const val achievementTypeIdArg = "achievement_type_id"

internal class AchievementInfoArgs(val achievementTypeId: Int) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(checkNotNull(savedStateHandle[achievementTypeIdArg]) as Int)
}

/**
 * Composable destination for [AchievementsInfoRoute]
 */
fun NavGraphBuilder.achievementsInfoScreen() {
    composable(
        route = "$achievementsInfoRoute?achievement_type_id={$achievementTypeIdArg}",
        arguments = listOf(
            navArgument(name = achievementTypeIdArg) {
                type = NavType.IntType
            },
        )
    ) {
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
        route = "$achievementsInfoRoute?achievement_type_id=${type.classValue}",
        navOptions = navOptions
    )
}