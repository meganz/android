package mega.privacy.android.app.presentation.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.airbnb.android.showkase.models.Showkase
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.theme.getBrowserIntent

class QASettingsFragment : PreferenceFragmentCompat() {
    private val composeBrowserPreferenceKey = "settings_qa_compose_browser"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_qa, rootKey)
    }


    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == composeBrowserPreferenceKey){
            startActivity(Showkase.getBrowserIntent(requireContext()))
        }
        return super.onPreferenceTreeClick(preference)
    }
}