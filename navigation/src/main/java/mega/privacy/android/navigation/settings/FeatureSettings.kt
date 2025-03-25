package mega.privacy.android.navigation.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * Feature settings
 *
 * @property settingsNavGraph
 */
interface FeatureSettings {
    val settingsNavGraph: NavGraphBuilder.(navHostController: NavHostController) -> Unit
}