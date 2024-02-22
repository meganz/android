package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.LegacySettingsCameraUploadsFragment

/**
 * Settings Activity class for Camera Uploads that holds [LegacySettingsCameraUploadsFragment]
 */
@Deprecated(message = "This is a legacy class that will be replaced by [SettingsCameraUploadsComposeActivity] once the migration to Jetpack Compose has been finished")
class LegacyCameraUploadsPreferencesActivity : PreferencesBaseActivity() {
    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (shouldRefreshSessionDueToSDK(true)) return
        setTitle(R.string.section_photo_sync)

        if (savedInstanceState == null) {
            LegacySettingsCameraUploadsFragment().also {
                replaceFragment(it)
            }
        }
    }
}
