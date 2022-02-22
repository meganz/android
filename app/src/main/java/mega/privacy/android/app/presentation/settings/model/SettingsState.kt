package mega.privacy.android.app.presentation.settings.model

data class SettingsState(
    val autoAcceptEnabled: Boolean,
    val autoAcceptChecked: Boolean,
    val multiFactorAuthChecked: Boolean,
    val multiFactorEnabled: Boolean,
    val multiFactorVisible: Boolean,
    val deleteAccountVisible: Boolean,
    val deleteEnabled: Boolean,
    val cameraUploadEnabled: Boolean,
    val chatEnabled: Boolean,
    val startScreen: Int,
    val hideRecentActivityChecked: Boolean,
) {
}
