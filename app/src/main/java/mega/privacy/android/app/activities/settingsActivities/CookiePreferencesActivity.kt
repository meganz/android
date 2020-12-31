package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.CookieSettingsFragment

@AndroidEntryPoint
class CookiePreferencesActivity : PreferencesBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aB.title = getString(R.string.dialog_cookie_settings)
        replaceFragment(CookieSettingsFragment())
    }
}
