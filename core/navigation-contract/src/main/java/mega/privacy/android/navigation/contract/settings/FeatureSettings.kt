package mega.privacy.android.navigation.contract.settings

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Feature settings
 *
 * @property settingsNavGraph
 */
interface FeatureSettings {
    val settingsNavGraph: NavGraphBuilder.(navigationHandler: NavigationHandler) -> Unit
}