package mega.privacy.android.app.presentation.settings.compose.appearance.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.navigation.settings.FeatureSettings

/**
 * Appearance feature setting
 */
data object AppearanceFeatureSetting : FeatureSettings {
    override val settingsNavGraph = NavGraphBuilder::appearanceSettingsGraph
}