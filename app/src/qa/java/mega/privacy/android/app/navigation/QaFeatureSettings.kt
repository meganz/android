package mega.privacy.android.app.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.app.presentation.settings.qaSettingsHomeDestination
import mega.privacy.android.navigation.settings.FeatureSettings

internal class QaFeatureSettings() :
    FeatureSettings {
    override val settingsNavGraph = NavGraphBuilder::qaSettingsHomeDestination
}