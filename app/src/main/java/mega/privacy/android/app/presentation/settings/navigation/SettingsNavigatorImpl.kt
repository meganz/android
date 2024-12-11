package mega.privacy.android.app.presentation.settings.navigation

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.INITIAL_PREFERENCE
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.NAVIGATE_TO_INITIAL_PREFERENCE
import mega.privacy.android.navigation.settings.SettingsNavigator
import mega.privacy.android.navigation.settings.arguments.TargetPreference
import javax.inject.Inject

/**
 * Settings navigator impl
 *
 * @constructor
 *
 * @param context
 */
class SettingsNavigatorImpl @Inject constructor() : SettingsNavigator {
    override fun openSettings(
        context: Context,
        targetPreference: TargetPreference?,
    ) {
        Intent(context, SettingsActivity::class.java).apply {
            putExtra(INITIAL_PREFERENCE, targetPreference?.preferenceId)
            putExtra(NAVIGATE_TO_INITIAL_PREFERENCE, targetPreference?.requiresNavigation)
            context.startActivity(this)
        }
    }
}