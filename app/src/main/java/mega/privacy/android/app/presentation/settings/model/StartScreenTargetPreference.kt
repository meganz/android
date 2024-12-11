package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Start screen
 */
data object StartScreenTargetPreference : TargetPreference {
    override val preferenceId = SettingsConstants.KEY_START_SCREEN
    override val requiresNavigation = true
}