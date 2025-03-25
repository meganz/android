package mega.privacy.android.app.activities.settingsActivities

import mega.privacy.android.shared.resources.R as sharedR
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsFragment

@AndroidEntryPoint
class CookiePreferencesActivity : PreferencesBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(sharedR.string.settings_cookie_setting_title_cookie_and_ad_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        replaceFragment(CookieSettingsFragment())
    }
}
