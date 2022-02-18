package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants

sealed class TargetPreference(val preferenceId: String, val requiresNavigation: Boolean) {
    object Storage : TargetPreference(SettingsConstants.KEY_STORAGE_FILE_MANAGEMENT, true)
    object QR : TargetPreference(SettingsConstants.KEY_QR_CODE_AUTO_ACCEPT, false)
    object StartScreen : TargetPreference(SettingsConstants.KEY_START_SCREEN, true)
}