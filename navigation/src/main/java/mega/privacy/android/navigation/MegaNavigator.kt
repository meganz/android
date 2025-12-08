package mega.privacy.android.navigation

import android.content.Context
import mega.privacy.android.navigation.settings.SettingsNavigator

/**
 * Mega navigator
 *
 */
interface MegaNavigator : AppNavigator, SettingsNavigator {
    fun launchMegaActivityIfNeeded(context: Context)
}