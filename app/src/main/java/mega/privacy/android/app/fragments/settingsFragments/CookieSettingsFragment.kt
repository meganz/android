package mega.privacy.android.app.fragments.settingsFragments

import android.os.Bundle
import androidx.preference.Preference
import mega.privacy.android.app.R

class CookieSettingsFragment : SettingsBaseFragment() {

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_cookie)
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        return super.onPreferenceClick(preference)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        return super.onPreferenceChange(preference, newValue)
    }
}
