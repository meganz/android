package mega.privacy.android.navigation.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * Feature settings
 *
 * @property entryPoints
 * @property settingsNavGraph - onBackPressed is the function called when navigating back at the root of the nested graph
 */
data class FeatureSettings(
    val entryPoints: List<SettingEntryPoint>,
    val settingsNavGraph: NavGraphBuilder.(onBackPressed: () -> Unit, navHostController: NavHostController) -> Unit,
)