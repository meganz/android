package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.SettingsPasscodeLockFragment
import mega.privacy.android.app.utils.StringResourcesUtils
import java.util.*

class PasscodePreferencesActivity : PreferencesBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        aB?.title = StringResourcesUtils.getString(R.string.settings_passcode_lock_switch)
            .toUpperCase(Locale.getDefault())

        replaceFragment(SettingsPasscodeLockFragment())
    }
}