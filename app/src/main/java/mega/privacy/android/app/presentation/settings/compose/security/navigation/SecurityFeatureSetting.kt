package mega.privacy.android.app.presentation.settings.compose.security.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.navigation.settings.FeatureSettings

/**
 * Appearance feature setting
 */
data object SecurityFeatureSetting : FeatureSettings {
    override val settingsNavGraph = NavGraphBuilder::securitySettings
}