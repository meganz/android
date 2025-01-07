package mega.privacy.android.app.presentation.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.home.SettingsHome
import mega.privacy.android.app.presentation.settings.home.settingsHome

@Serializable
internal object SettingsGraph

/**
 * Settings graph
 *
 */
fun NavGraphBuilder.settingsGraph(
    onBackPressed: () -> Unit,
    navController: NavHostController,
) {
    navigation<SettingsGraph>(
        startDestination = SettingsHome()
    ) {
        settingsHome(onBackPressed)
    }
}