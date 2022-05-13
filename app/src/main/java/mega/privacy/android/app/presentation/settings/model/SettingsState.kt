package mega.privacy.android.app.presentation.settings.model

/**
 * Settings ui state
 *
 * @property autoAcceptEnabled
 * @property autoAcceptChecked
 * @property multiFactorAuthChecked
 * @property multiFactorEnabled
 * @property multiFactorVisible
 * @property deleteAccountVisible
 * @property deleteEnabled
 * @property cameraUploadEnabled
 * @property chatEnabled
 * @property startScreen
 * @property hideRecentActivityChecked
 * @property email
 * @property accountType
 */
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
    val email: String,
    val accountType: Int,
) {
}
