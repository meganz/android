package mega.privacy.android.app.presentation.settings

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.airbnb.android.showkase.models.Showkase
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.theme.getBrowserIntent

@AndroidEntryPoint
class QASettingsFragment : PreferenceFragmentCompat() {
    val viewModel by viewModels<QASettingViewModel>()

    private val composeBrowserPreferenceKey = "settings_qa_compose_browser"
    private val checkForUpdatesPreferenceKey = "settings_qa_check_update"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_qa, rootKey)
    }


    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            composeBrowserPreferenceKey -> {
                startActivity(Showkase.getBrowserIntent(requireContext()))
                true
            }
            checkForUpdatesPreferenceKey -> {
                viewModel.checkUpdatePressed()
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }
}