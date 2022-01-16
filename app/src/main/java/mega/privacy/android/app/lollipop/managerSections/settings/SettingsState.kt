package mega.privacy.android.app.lollipop.managerSections.settings

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
)
