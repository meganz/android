package mega.privacy.android.navigation.settings

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * Feature settings
 *
 * @property entryPoints
 * @property settingsNavGraph
 */
interface FeatureSettings {
    val entryPoints: List<SettingEntryPoint>
    val settingsNavGraph: NavGraphBuilder.(navHostController: NavHostController) -> Unit

    /**
     * Get title for destination
     *
     * @param entry
     * @return title for the destination if found, else null
     */
    fun getTitleForDestination(entry: NavBackStackEntry): String?
}