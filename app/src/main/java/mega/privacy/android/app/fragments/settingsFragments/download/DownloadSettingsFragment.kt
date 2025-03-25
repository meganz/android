package mega.privacy.android.app.fragments.settingsFragments.download

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.TwoLineCheckPreference
import mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_ASK_ME_ALWAYS
import mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_DOWNLOAD_LOCATION
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import timber.log.Timber

/**
 * The fragment for download settings
 */
@AndroidEntryPoint
class DownloadSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel by viewModels<DownloadSettingsViewModel>()

    private var downloadLocation: Preference? = null
    private var storageAskMeAlways: TwoLineCheckPreference? = null

    private lateinit var selectFolderLauncher: ActivityResultLauncher<Uri?>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_download)
        downloadLocation = findPreference(KEY_STORAGE_DOWNLOAD_LOCATION)
        storageAskMeAlways = findPreference(KEY_STORAGE_ASK_ME_ALWAYS)
        setPreferencesClickListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectUIState()
        selectFolderLauncher = launchFolderPicker(
            onCancel = { Timber.w("REQUEST_DOWNLOAD_FOLDER - canceled") }
        ) { selectedFolderUri ->
            viewModel.setDownloadLocation(selectedFolderUri.toString())
        }
    }

    /**
     * Collect UI State from the View Model to update the fragment view
     */
    private fun collectUIState() {
        viewLifecycleOwner.collectFlow(viewModel.uiState) { (downloadPath, askAlwaysChecked) ->
            downloadLocation?.summary = downloadPath
            storageAskMeAlways?.isChecked = askAlwaysChecked

            downloadLocation?.let { dl ->
                if (askAlwaysChecked) {
                    preferenceScreen.removePreference(dl)
                } else {
                    preferenceScreen.addPreference(dl)
                }
            }
        }
    }

    private fun setPreferencesClickListener() {
        downloadLocation?.onPreferenceClickListener = launchFolderSelectionScreen()
        storageAskMeAlways?.onPreferenceClickListener = onStorageAskMeAlwaysCheckChanged()
    }

    private fun launchFolderSelectionScreen() = Preference.OnPreferenceClickListener {
        selectFolderLauncher.launch(null)
        true
    }

    private fun onStorageAskMeAlwaysCheckChanged() = Preference.OnPreferenceClickListener {
        storageAskMeAlways?.let { pref ->
            viewModel.onStorageAskAlwaysChanged(pref.isChecked)
        }
        true
    }
}