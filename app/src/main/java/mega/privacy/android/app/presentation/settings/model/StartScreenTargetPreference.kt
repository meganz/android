package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Start screen
 */
val startScreenTargetPreference = TargetPreference (
    preferenceId = SettingsConstants.KEY_START_SCREEN,
    requiresNavigation = true,
    rootKey = "appearance",
)