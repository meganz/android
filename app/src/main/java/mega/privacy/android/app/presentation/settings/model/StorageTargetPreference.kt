package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Storage
 */
val storageTargetPreference = TargetPreference(
    preferenceId = SettingsConstants.KEY_STORAGE_FILE_MANAGEMENT,
    requiresNavigation = true,
    rootKey = "file_management",
)