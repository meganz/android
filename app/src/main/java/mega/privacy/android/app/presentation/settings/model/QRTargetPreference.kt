package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Qr
 */
val qrTargetPreference = TargetPreference(
    preferenceId = SettingsConstants.KEY_QR_CODE_AUTO_ACCEPT,
    requiresNavigation = false,
    rootKey = "security",
)