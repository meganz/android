package mega.privacy.android.app.activities.settingsActivities

import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.SettingsPasscodeLockFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import java.util.*

class PasscodePreferencesActivity : PreferencesBaseActivity() {

    private lateinit var sttPasscodeLockFragment: SettingsPasscodeLockFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        aB?.title = StringResourcesUtils.getString(R.string.settings_pin_lock_switch)
            .toUpperCase(Locale.getDefault())

        sttPasscodeLockFragment = SettingsPasscodeLockFragment()
        replaceFragment(sttPasscodeLockFragment)
    }
}