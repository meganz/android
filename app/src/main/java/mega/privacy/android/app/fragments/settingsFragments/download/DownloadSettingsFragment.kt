package mega.privacy.android.app.fragments.settingsFragments.download

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.TwoLineCheckPreference
import mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_ASK_ME_ALWAYS
import mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_DOWNLOAD_LOCATION
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.FileStorageActivity.PickFolderType
import timber.log.Timber

/**
 * The fragment for download settings
 */
@AndroidEntryPoint
class DownloadSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel by viewModels<DownloadSettingsViewModel>()

    private var downloadLocation: Preference? = null
    private var storageAskMeAlways: TwoLineCheckPreference? = null

    private val downloadFolderActivityResult =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.data != null) {
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.setDownloadLocation(result.data?.getStringExtra(FileStorageActivity.EXTRA_PATH))
                } else if (result.resultCode == Activity.RESULT_CANCELED) {
                    Timber.w("REQUEST_DOWNLOAD_FOLDER - canceled")
                }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_download)
        downloadLocation = findPreference(KEY_STORAGE_DOWNLOAD_LOCATION)
        storageAskMeAlways = findPreference(KEY_STORAGE_ASK_ME_ALWAYS)
        setPreferencesClickListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectUIState()
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
        val intent = Intent(context, FileStorageActivity::class.java).apply {
            action = FileStorageActivity.Mode.PICK_FOLDER.action
            putExtra(
                FileStorageActivity.PICK_FOLDER_TYPE,
                PickFolderType.DOWNLOAD_FOLDER.folderType
            )
        }

        downloadFolderActivityResult.launch(intent)
        true
    }

    private fun onStorageAskMeAlwaysCheckChanged() = Preference.OnPreferenceClickListener {
        storageAskMeAlways?.let { pref ->
            viewModel.onStorageAskAlwaysChanged(pref.isChecked)
        }
        true
    }
}