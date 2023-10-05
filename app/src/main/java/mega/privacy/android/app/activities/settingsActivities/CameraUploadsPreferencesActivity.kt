package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.SettingsCameraUploadsFragment

/**
 * Settings Activity class for Camera Uploads that holds [SettingsCameraUploadsFragment]
 */
class CameraUploadsPreferencesActivity : PreferencesBaseActivity() {
    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (shouldRefreshSessionDueToSDK(true)) return

        setTitle(R.string.section_photo_sync)
        SettingsCameraUploadsFragment().also {
            replaceFragment(it)
        }
    }
}
