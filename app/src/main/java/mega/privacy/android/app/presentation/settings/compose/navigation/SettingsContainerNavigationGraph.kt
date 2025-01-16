package mega.privacy.android.app.presentation.settings.compose.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.container.SettingsContainer
import mega.privacy.android.app.presentation.settings.compose.container.settingsContainer

@Serializable
internal object SettingsGraph

/**
 * Settings graph
 *
 */
fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    onNavigateBack: () -> Unit,
) {
    navigation<SettingsGraph>(
        startDestination = SettingsContainer()
    ) {
        settingsContainer(navController, onNavigateBack)
    }
}