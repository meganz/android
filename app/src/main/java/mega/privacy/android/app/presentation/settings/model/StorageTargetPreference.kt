package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Storage
 */
data object StorageTargetPreference : TargetPreference {
    override val preferenceId = SettingsConstants.KEY_STORAGE_FILE_MANAGEMENT
    override val requiresNavigation = true
}