package mega.privacy.android.app.presentation.settings.advanced

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.components.TwoLineCheckPreference

@AndroidEntryPoint
class SettingsAdvancedFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {
    private val keyHttpsOnly = "settings_use_https_only"

    private val viewModel: SettingsAdvancedViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_advanced)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            keyHttpsOnly -> {
                viewModel.useHttpsPreferenceChanged((preference as TwoLineCheckPreference).isChecked)
            }
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED){
                viewModel.state.collect{ (useHttpsChecked, useHttpsEnabled) ->
                    findPreference<TwoLineCheckPreference>(keyHttpsOnly)?.let {
                        it.onPreferenceClickListener = this@SettingsAdvancedFragment
                        it.isEnabled = useHttpsEnabled
                        it.isChecked = useHttpsChecked
                    }
                }
            }
        }
    }

}