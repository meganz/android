package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import androidx.fragment.app.commit
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.startscreen.StartScreenSettingsFragment
import mega.privacy.android.app.utils.StringResourcesUtils

class StartScreenPreferencesActivity : PreferencesBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = StringResourcesUtils.getString(R.string.configure_start_screen)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, StartScreenSettingsFragment())
        }
    }
}