package mega.privacy.android.navigation.settings

import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Settings navigator
 */
interface SettingsNavigator {

    /**
     * Open settings
     *
     * @param targetPreference
     */
    fun openSettings(targetPreference: TargetPreference? = null)
}