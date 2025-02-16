package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.download.DownloadSettingsFragment

@AndroidEntryPoint
class DownloadPreferencesActivity : PreferencesBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.download_location)
        replaceFragment(DownloadSettingsFragment())
    }
}
