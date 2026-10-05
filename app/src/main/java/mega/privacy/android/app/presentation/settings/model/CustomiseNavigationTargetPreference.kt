package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Customise navigation
 */
val customiseNavigationTargetPreference = TargetPreference(
    preferenceId = SettingsConstants.KEY_CUSTOMISE_NAVIGATION,
    requiresNavigation = false,
    rootKey = "appearance",
)
