package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.download.DownloadSettingsFragment
import mega.privacy.android.app.presentation.security.PasscodeCheck
import javax.inject.Inject

@AndroidEntryPoint
class DownloadPreferencesActivity : PreferencesBaseActivity() {
    /**
     * A [LifecycleEventObserver] to display a passcode screen when the app is resumed
     */
    @Inject
    lateinit var passcodeFacade: PasscodeCheck

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.download_location)
        replaceFragment(DownloadSettingsFragment())
    }
}
