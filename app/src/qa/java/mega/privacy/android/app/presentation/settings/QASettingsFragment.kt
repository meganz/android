package mega.privacy.android.app.presentation.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import mega.privacy.android.app.R

class QASettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_qa, rootKey)
    }
}