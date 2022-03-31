package mega.privacy.android.app.presentation.settings.advanced.model

/**
 * Settings advanced state
 *
 * @property useHttpsChecked is use https checked, default false
 * @property useHttpsEnabled is use https enabled, default false
 * @constructor Create default Settings advanced state
 */
data class SettingsAdvancedState(
    val useHttpsChecked: Boolean = false,
    val useHttpsEnabled: Boolean = false,
)
