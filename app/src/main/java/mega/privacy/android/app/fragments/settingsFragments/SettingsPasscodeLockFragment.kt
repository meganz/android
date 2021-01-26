package mega.privacy.android.app.fragments.settingsFragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity.Companion.ACTION_RESET_PASSCODE_LOCK
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity.Companion.ACTION_SET_PASSCODE_LOCK
import mega.privacy.android.app.constants.SettingsConstants.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.TextUtil.isTextEmpty

class SettingsPasscodeLockFragment : SettingsBaseFragment() {

    private var passcodeSwitch: SwitchPreferenceCompat? = null
    private var resetPasscode: Preference? = null
    private var requirePasscode: Preference? = null

    private var passcodeLock = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_passcode)
        passcodeSwitch = findPreference(KEY_PASSCODE_ENABLE)
        passcodeSwitch?.setOnPreferenceClickListener {
            if (passcodeLock) {
                disablePasscode()
            } else {
                passcodeSwitch?.isChecked = false
                intentToPasscodeLock(false)
            }

            true
        }

        resetPasscode = findPreference(KEY_RESET_PASSCODE)
        resetPasscode?.apply {
            setOnPreferenceClickListener {
                intentToPasscodeLock(true)
                true
            }

            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    dbH.passcodeLockCode = newValue.toString()
                    true
                }
        }

        requirePasscode = findPreference(KEY_REQUIRE_PASSCODE)
        requirePasscode?.setOnPreferenceClickListener {

            true
        }

        prefs = dbH.preferences

        if (prefs == null || prefs.passcodeLockEnabled == null
            || !prefs.passcodeLockEnabled.toBoolean() || isTextEmpty(prefs.passcodeLockCode)
        ) {
            disablePasscode()
        } else {
            enablePasscode()
        }
    }

    private fun enablePasscode() {
        passcodeLock = true
        passcodeSwitch?.isChecked = true
        preferenceScreen.addPreference(resetPasscode)
        preferenceScreen.addPreference(requirePasscode)
    }

    private fun disablePasscode() {
        passcodeLock = false
        passcodeSwitch?.isChecked = false
        preferenceScreen.removePreference(resetPasscode)
        preferenceScreen.removePreference(requirePasscode)
        dbH.isPasscodeLockEnabled = false
        dbH.passcodeLockCode = ""
    }

    private fun intentToPasscodeLock(reset: Boolean) {
        val intent = Intent(context, PasscodeLockActivity::class.java)
        intent.action = if (reset) ACTION_RESET_PASSCODE_LOCK else ACTION_SET_PASSCODE_LOCK
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