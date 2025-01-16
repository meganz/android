package mega.privacy.android.app.presentation.settings.compose.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.home.SettingsHome
import mega.privacy.android.app.presentation.settings.compose.home.settingsHome

@Serializable
internal object SettingsHomeGraph

/**
 * Settings graph
 *
 */
fun NavGraphBuilder.settingsHomeGraph(
    navController: NavHostController,
    initialSetting: String?
) {
    navigation<SettingsHomeGraph>(
        startDestination = SettingsHome(initialSetting)
    ) {
        settingsHome(navController)
    }
}