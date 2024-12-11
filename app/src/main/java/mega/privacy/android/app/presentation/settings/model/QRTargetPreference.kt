package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Qr
 */
data object QRTargetPreference : TargetPreference {
    override val preferenceId = SettingsConstants.KEY_QR_CODE_AUTO_ACCEPT
    override val requiresNavigation = false
}