package mega.privacy.android.app.fragments.settingsFragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.PasscodeActivity
import mega.privacy.android.app.activities.settingsActivities.PasscodeActivity.Companion.ACTION_RESET_PIN_LOCK
import mega.privacy.android.app.activities.settingsActivities.PasscodeActivity.Companion.ACTION_SET_PIN_LOCK
import mega.privacy.android.app.constants.SettingsConstants.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.TextUtil.isTextEmpty

class SettingsPasscodeLockFragment : SettingsBaseFragment() {

    private var passcodeSwitch: SwitchPreferenceCompat? = null
    private var resetPasscode: Preference? = null
    private var requirePasscode: Preference? = null

    private var pinLock = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_passcode)
        passcodeSwitch = findPreference(KEY_PASSCODE_ENABLE)
        passcodeSwitch?.setOnPreferenceClickListener {
            if (pinLock) {
                disablePasscode()
            } else intentToPinLock(false)

            true
        }

        resetPasscode = findPreference(KEY_RESET_PASSCODE)
        resetPasscode?.apply {
            setOnPreferenceClickListener {
                intentToPinLock(true)
                true
            }

            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    dbH.setPinLockCode(newValue.toString())
                    true
                }
        }

        requirePasscode = findPreference(KEY_REQUIRE_PASSCODE)
        requirePasscode?.setOnPreferenceClickListener {

            true
        }

        prefs = dbH.preferences

        if (prefs == null || prefs.pinLockEnabled == null
            || !prefs.pinLockEnabled.toBoolean() || isTextEmpty(prefs.pinLockCode)
        ) {
            disablePasscode()
        } else {
            enablePasscode()
        }
    }

    private fun enablePasscode() {
        pinLock = true
        passcodeSwitch?.isChecked = true
        preferenceScreen.addPreference(resetPasscode)
        preferenceScreen.addPreference(requirePasscode)
    }

    private fun disablePasscode() {
        pinLock = false
        passcodeSwitch?.isChecked = false
        preferenceScreen.removePreference(resetPasscode)
        preferenceScreen.removePreference(requirePasscode)
        dbH.setPinLockEnabled(false)
        dbH.setPinLockCode("")
    }

    private fun intentToPinLock(reset: Boolean) {
        val intent = Intent(context, PasscodeActivity::class.java)
        intent.action = if (reset) ACTION_RESET_PIN_LOCK else ACTION_SET_PIN_LOCK
        startActivityForResult(intent, Constants.SET_PIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != Constants.SET_PIN) return

        if (resultCode == Activity.RESULT_OK) {
            enablePasscode()
        } else {
            LogUtil.logWarning("Set PIN ERROR")
        }
    }
}