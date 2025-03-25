package mega.privacy.android.navigation.settings

import android.content.Context
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
    fun openSettings(context: Context, targetPreference: TargetPreference? = null)
}