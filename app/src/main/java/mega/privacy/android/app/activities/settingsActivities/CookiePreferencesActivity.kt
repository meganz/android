package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.CookieSettingsFragment

class CookiePreferencesActivity : PreferencesBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aB.title = getString(R.string.dialog_cookie_settings)
        replaceFragment(CookieSettingsFragment())
    }
}
